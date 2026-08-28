package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.net.Uri
import com.example.data.model.ScanFilter
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

object ScanImageProcessor {

    /**
     * Automatically detects document boundary corners on a given bitmap.
     * Returns normalized coordinates (0f..1f) for Top-Left, Top-Right, Bottom-Right, Bottom-Left.
     */
    fun detectDocumentCorners(bitmap: Bitmap): QuadrilateralCorners {
        val w = bitmap.width
        val h = bitmap.height

        if (w < 50 || h < 50) {
            return QuadrilateralCorners(
                0.04f to 0.04f,
                0.96f to 0.04f,
                0.96f to 0.96f,
                0.04f to 0.96f
            )
        }

        // Downsample for fast pixel scanning
        val sampleW = 200
        val sampleH = (200f * h / w).toInt().coerceAtLeast(100)
        val scaled = Bitmap.createScaledBitmap(bitmap, sampleW, sampleH, false)
        val pixels = IntArray(sampleW * sampleH)
        scaled.getPixels(pixels, 0, sampleW, 0, 0, sampleW, sampleH)

        var top = 0
        var bottom = sampleH - 1
        var left = 0
        var right = sampleW - 1

        // Scan from top down
        topLoop@ for (y in 0 until sampleH / 3) {
            for (x in 0 until sampleW) {
                val pixel = pixels[y * sampleW + x]
                val r = (pixel shr 16) and 0xFF
                val g = (pixel shr 8) and 0xFF
                val b = pixel and 0xFF
                val lum = 0.299f * r + 0.587f * g + 0.114f * b
                if (lum > 75) {
                    top = y
                    break@topLoop
                }
            }
        }

        // Scan from bottom up
        bottomLoop@ for (y in sampleH - 1 downTo (2 * sampleH / 3)) {
            for (x in 0 until sampleW) {
                val pixel = pixels[y * sampleW + x]
                val r = (pixel shr 16) and 0xFF
                val g = (pixel shr 8) and 0xFF
                val b = pixel and 0xFF
                val lum = 0.299f * r + 0.587f * g + 0.114f * b
                if (lum > 75) {
                    bottom = y
                    break@bottomLoop
                }
            }
        }

        // Scan from left to right
        leftLoop@ for (x in 0 until sampleW / 3) {
            for (y in 0 until sampleH) {
                val pixel = pixels[y * sampleW + x]
                val r = (pixel shr 16) and 0xFF
                val g = (pixel shr 8) and 0xFF
                val b = pixel and 0xFF
                val lum = 0.299f * r + 0.587f * g + 0.114f * b
                if (lum > 75) {
                    left = x
                    break@leftLoop
                }
            }
        }

        // Scan from right to left
        rightLoop@ for (x in sampleW - 1 downTo (2 * sampleW / 3)) {
            for (y in 0 until sampleH) {
                val pixel = pixels[y * sampleW + x]
                val r = (pixel shr 16) and 0xFF
                val g = (pixel shr 8) and 0xFF
                val b = pixel and 0xFF
                val lum = 0.299f * r + 0.587f * g + 0.114f * b
                if (lum > 75) {
                    right = x
                    break@rightLoop
                }
            }
        }

        val normL = (left.toFloat() / sampleW).coerceIn(0.02f, 0.20f)
        val normR = (right.toFloat() / sampleW).coerceIn(0.80f, 0.98f)
        val normT = (top.toFloat() / sampleH).coerceIn(0.02f, 0.20f)
        val normB = (bottom.toFloat() / sampleH).coerceIn(0.80f, 0.98f)

        return QuadrilateralCorners(
            normL to normT,
            normR to normT,
            normR to normB,
            normL to normB
        )
    }

    /**
     * Applies perspective transformation (dewarp/crop) to flat document box.
     */
    fun perspectiveCrop(
        bitmap: Bitmap,
        tlNorm: Pair<Float, Float>,
        trNorm: Pair<Float, Float>,
        brNorm: Pair<Float, Float>,
        blNorm: Pair<Float, Float>
    ): Bitmap {
        val w = bitmap.width.toFloat()
        val h = bitmap.height.toFloat()

        val srcTL = floatArrayOf(tlNorm.first.coerceIn(0f, 1f) * w, tlNorm.second.coerceIn(0f, 1f) * h)
        val srcTR = floatArrayOf(trNorm.first.coerceIn(0f, 1f) * w, trNorm.second.coerceIn(0f, 1f) * h)
        val srcBR = floatArrayOf(brNorm.first.coerceIn(0f, 1f) * w, brNorm.second.coerceIn(0f, 1f) * h)
        val srcBL = floatArrayOf(blNorm.first.coerceIn(0f, 1f) * w, blNorm.second.coerceIn(0f, 1f) * h)

        val topW = Math.hypot((srcTR[0] - srcTL[0]).toDouble(), (srcTR[1] - srcTL[1]).toDouble()).toFloat()
        val botW = Math.hypot((srcBR[0] - srcBL[0]).toDouble(), (srcBR[1] - srcBL[1]).toDouble()).toFloat()
        val targetW = maxOf(topW, botW).coerceAtLeast(100f)

        val leftH = Math.hypot((srcBL[0] - srcTL[0]).toDouble(), (srcBL[1] - srcTL[1]).toDouble()).toFloat()
        val rightH = Math.hypot((srcBR[0] - srcTR[0]).toDouble(), (srcBR[1] - srcTR[1]).toDouble()).toFloat()
        val targetH = maxOf(leftH, rightH).coerceAtLeast(100f)

        val resultBitmap = Bitmap.createBitmap(targetW.toInt(), targetH.toInt(), Bitmap.Config.ARGB_8888)
        val canvas = Canvas(resultBitmap)

        val matrix = Matrix()
        val srcPoints = floatArrayOf(
            srcTL[0], srcTL[1],
            srcTR[0], srcTR[1],
            srcBR[0], srcBR[1],
            srcBL[0], srcBL[1]
        )
        val dstPoints = floatArrayOf(
            0f, 0f,
            targetW, 0f,
            targetW, targetH,
            0f, targetH
        )

        val success = matrix.setPolyToPoly(srcPoints, 0, dstPoints, 0, 4)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

        if (success) {
            canvas.drawBitmap(bitmap, matrix, paint)
        } else {
            val minX = minOf(srcTL[0], srcTR[0], srcBR[0], srcBL[0]).toInt().coerceIn(0, bitmap.width - 1)
            val minY = minOf(srcTL[1], srcTR[1], srcBR[1], srcBL[1]).toInt().coerceIn(0, bitmap.height - 1)
            val maxX = maxOf(srcTL[0], srcTR[0], srcBR[0], srcBL[0]).toInt().coerceIn(minX + 1, bitmap.width)
            val maxY = maxOf(srcTL[1], srcTR[1], srcBR[1], srcBL[1]).toInt().coerceIn(minY + 1, bitmap.height)
            val rectBmp = Bitmap.createBitmap(bitmap, minX, minY, maxX - minX, maxY - minY)
            canvas.drawBitmap(rectBmp, 0f, 0f, paint)
        }

        return resultBitmap
    }

    /**
     * Applies document scan filters & image enhancement (Magic Color, B&W, Grayscale, Lighten, Brightness/Contrast).
     */
    fun applyScanFilter(
        bitmap: Bitmap,
        filter: ScanFilter,
        brightness: Float = 0f,
        contrast: Float = 0f
    ): Bitmap {
        val result = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

        val cm = ColorMatrix()

        when (filter) {
            ScanFilter.ORIGINAL -> {
                cm.reset()
            }
            ScanFilter.MAGIC_COLOR -> {
                // Magic color: sharp contrast, clean white background, vibrant text colors
                cm.set(floatArrayOf(
                    1.2f, -0.1f, -0.1f, 0f, 15f,
                    -0.1f, 1.2f, -0.1f, 0f, 15f,
                    -0.1f, -0.1f, 1.2f, 0f, 15f,
                    0f,    0f,    0f,    1f, 0f
                ))
            }
            ScanFilter.BW -> {
                // Crisp high-contrast B&W document scan
                val bwMatrix = ColorMatrix().apply { setSaturation(0f) }
                val scale = 2.4f
                val translate = (-0.5f * scale + 0.5f) * 255f + 15f
                val contrastMatrix = ColorMatrix(floatArrayOf(
                    scale, 0f, 0f, 0f, translate,
                    0f, scale, 0f, 0f, translate,
                    0f, 0f, scale, 0f, translate,
                    0f, 0f, 0f, 1f, 0f
                ))
                bwMatrix.postConcat(contrastMatrix)
                cm.set(bwMatrix)
            }
            ScanFilter.GRAYSCALE -> {
                cm.setSaturation(0f)
                val grScale = 1.2f
                val grTranslate = -10f
                cm.postConcat(ColorMatrix(floatArrayOf(
                    grScale, 0f, 0f, 0f, grTranslate,
                    0f, grScale, 0f, 0f, grTranslate,
                    0f, 0f, grScale, 0f, grTranslate,
                    0f, 0f, 0f, 1f, 0f
                )))
            }
            ScanFilter.LIGHTEN -> {
                // Removes dark background / shadows
                cm.set(floatArrayOf(
                    1.15f, 0f, 0f, 0f, 30f,
                    0f, 1.15f, 0f, 0f, 30f,
                    0f, 0f, 1.15f, 0f, 30f,
                    0f, 0f, 0f, 1f, 0f
                ))
            }
        }

        if (brightness != 0f || contrast != 0f) {
            val c = 1f + contrast
            val b = brightness * 255f
            val adjCm = ColorMatrix(floatArrayOf(
                c, 0f, 0f, 0f, b,
                0f, c, 0f, 0f, b,
                0f, 0f, c, 0f, b,
                0f, 0f, 0f, 1f, 0f
            ))
            cm.postConcat(adjCm)
        }

        paint.colorFilter = ColorMatrixColorFilter(cm)
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        return result
    }

    /**
     * Helper to load bitmap from Uri string
     */
    fun loadBitmapFromUri(context: Context, uriString: String): Bitmap? {
        return try {
            val uri = if (uriString.startsWith("/")) {
                Uri.fromFile(File(uriString))
            } else {
                Uri.parse(uriString)
            }
            val inputStream = context.contentResolver.openInputStream(uri)
            BitmapFactory.decodeStream(inputStream)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Saves bitmap to cache file and returns file Uri string
     */
    fun saveBitmapToCache(context: Context, bitmap: Bitmap, prefix: String = "scan"): String {
        val file = File(
            context.cacheDir,
            "${prefix}_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}.jpg"
        )
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)
        }
        return Uri.fromFile(file).toString()
    }
}

data class QuadrilateralCorners(
    val tl: Pair<Float, Float>,
    val tr: Pair<Float, Float>,
    val br: Pair<Float, Float>,
    val bl: Pair<Float, Float>
)
