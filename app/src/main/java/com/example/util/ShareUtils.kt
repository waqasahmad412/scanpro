package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

object ShareUtils {

    private const val LOG_TAG = "ShareUtils"

    private fun getFileProviderUri(context: Context, file: File): Uri {
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }

    /**
     * Generates a multi-page PDF file from a list of image URIs, applying optional watermark and signature.
     */
    fun generatePdfFile(
        context: Context,
        docTitle: String,
        imageUris: List<String>,
        watermarkText: String? = null,
        signatureBitmap: Bitmap? = null
    ): File? {
        if (imageUris.isEmpty()) return null

        val pdfDocument = PdfDocument()
        val sanitizedTitle = docTitle.replace("[^a-zA-Z0-9_]".toRegex(), "_")

        try {
            imageUris.forEachIndexed { index, uriString ->
                val baseBitmap = loadBitmapFromUri(context, uriString) ?: return@forEachIndexed
                val bitmap = applyWatermarkAndSignature(baseBitmap, watermarkText, signatureBitmap)

                // Standard A4 dimensions in points: 595 x 842
                val pageInfo = PdfDocument.PageInfo.Builder(595, 842, index + 1).create()
                val page = pdfDocument.startPage(pageInfo)
                val canvas = page.canvas

                // Scale bitmap to fit A4 page while preserving aspect ratio
                val scale = minOf(
                    595f / bitmap.width.toFloat(),
                    842f / bitmap.height.toFloat()
                )
                val scaledW = bitmap.width * scale
                val scaledH = bitmap.height * scale
                val dx = (595f - scaledW) / 2f
                val dy = (842f - scaledH) / 2f

                val destRect = android.graphics.RectF(dx, dy, dx + scaledW, dy + scaledH)
                canvas.drawBitmap(bitmap, null, destRect, null)
                pdfDocument.finishPage(page)
            }

            val pdfDir = File(context.cacheDir, "shared_pdfs")
            if (!pdfDir.exists()) pdfDir.mkdirs()

            val pdfFile = File(pdfDir, "${sanitizedTitle}_${System.currentTimeMillis()}.pdf")
            FileOutputStream(pdfFile).use { out ->
                pdfDocument.writeTo(out)
            }
            pdfDocument.close()
            return pdfFile
        } catch (e: Exception) {
            Log.e(LOG_TAG, "Failed to generate PDF: ${e.message}", e)
            pdfDocument.close()
            return null
        }
    }

    /**
     * Share document as PDF to any app or specifically WhatsApp
     */
    fun shareDocumentAsPdf(
        context: Context,
        docTitle: String,
        imageUris: List<String>,
        targetWhatsApp: Boolean = false,
        watermarkText: String? = null,
        signatureBitmap: Bitmap? = null
    ) {
        val pdfFile = generatePdfFile(context, docTitle, imageUris, watermarkText, signatureBitmap)
        if (pdfFile == null || !pdfFile.exists()) {
            Toast.makeText(context, "Could not generate PDF for sharing", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val contentUri = getFileProviderUri(context, pdfFile)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, contentUri)
                putExtra(Intent.EXTRA_SUBJECT, docTitle)
                putExtra(Intent.EXTRA_TEXT, "Scanned Document: $docTitle (ScanPro AI)")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            if (targetWhatsApp) {
                intent.setPackage("com.whatsapp")
            }

            val chooser = Intent.createChooser(intent, "Share $docTitle via")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: Exception) {
            Log.e(LOG_TAG, "Sharing failed: ${e.message}")
            if (targetWhatsApp) {
                // Fallback to standard chooser
                shareDocumentAsPdf(context, docTitle, imageUris, targetWhatsApp = false, watermarkText = watermarkText, signatureBitmap = signatureBitmap)
            } else {
                Toast.makeText(context, "Share failed: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Share document images as JPEGs
     */
    fun shareDocumentAsImages(
        context: Context,
        docTitle: String,
        imageUris: List<String>,
        targetWhatsApp: Boolean = false,
        watermarkText: String? = null,
        signatureBitmap: Bitmap? = null
    ) {
        if (imageUris.isEmpty()) return

        try {
            val sharedImageDir = File(context.cacheDir, "shared_images")
            if (!sharedImageDir.exists()) sharedImageDir.mkdirs()

            val contentUris = ArrayList<Uri>()
            imageUris.forEachIndexed { idx, uriStr ->
                val baseBitmap = loadBitmapFromUri(context, uriStr)
                if (baseBitmap != null) {
                    val composited = applyWatermarkAndSignature(baseBitmap, watermarkText, signatureBitmap)
                    val imgFile = File(sharedImageDir, "page_${idx + 1}_${System.currentTimeMillis()}.jpg")
                    FileOutputStream(imgFile).use { out ->
                        composited.compress(Bitmap.CompressFormat.JPEG, 92, out)
                    }
                    contentUris.add(getFileProviderUri(context, imgFile))
                } else {
                    val file = File(Uri.parse(uriStr).path ?: "")
                    if (file.exists()) {
                        contentUris.add(getFileProviderUri(context, file))
                    } else {
                        contentUris.add(Uri.parse(uriStr))
                    }
                }
            }

            val intent = if (contentUris.size == 1) {
                Intent(Intent.ACTION_SEND).apply {
                    type = "image/jpeg"
                    putExtra(Intent.EXTRA_STREAM, contentUris.first())
                }
            } else {
                Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                    type = "image/jpeg"
                    putParcelableArrayListExtra(Intent.EXTRA_STREAM, contentUris)
                }
            }

            intent.putExtra(Intent.EXTRA_SUBJECT, docTitle)
            intent.putExtra(Intent.EXTRA_TEXT, "Scanned Image: $docTitle")
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

            if (targetWhatsApp) {
                intent.setPackage("com.whatsapp")
            }

            val chooser = Intent.createChooser(intent, "Share Images via")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: Exception) {
            Log.e(LOG_TAG, "Image share failed: ${e.message}")
            if (targetWhatsApp) {
                shareDocumentAsImages(context, docTitle, imageUris, targetWhatsApp = false, watermarkText = watermarkText, signatureBitmap = signatureBitmap)
            } else {
                Toast.makeText(context, "Share failed: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun applyWatermarkAndSignature(
        sourceBitmap: Bitmap,
        watermarkText: String?,
        signatureBitmap: Bitmap?
    ): Bitmap {
        if (watermarkText.isNullOrBlank() && signatureBitmap == null) return sourceBitmap

        val mutableBitmap = sourceBitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = android.graphics.Canvas(mutableBitmap)

        val w = mutableBitmap.width.toFloat()
        val h = mutableBitmap.height.toFloat()

        // Apply Watermark Text diagonally
        if (!watermarkText.isNullOrBlank()) {
            val paint = android.graphics.Paint().apply {
                isAntiAlias = true
                color = android.graphics.Color.RED
                alpha = 85 // semi-transparent
                textSize = w * 0.08f
                typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
                textAlign = android.graphics.Paint.Align.CENTER
            }

            canvas.save()
            canvas.rotate(-30f, w / 2f, h / 2f)
            canvas.drawText(watermarkText, w / 2f, h / 2f, paint)
            canvas.restore()
        }

        // Apply Signature at bottom right
        if (signatureBitmap != null) {
            val sigW = w * 0.35f
            val sigH = sigW * (signatureBitmap.height.toFloat() / signatureBitmap.width.toFloat().coerceAtLeast(1f))
            val left = w - sigW - (w * 0.08f)
            val top = h - sigH - (h * 0.08f)

            val destRect = android.graphics.RectF(left, top, left + sigW, top + sigH)
            canvas.drawBitmap(signatureBitmap, null, destRect, null)
        }

        return mutableBitmap
    }

    /**
     * Share extracted OCR text
     */
    fun shareOcrText(context: Context, docTitle: String, text: String, targetWhatsApp: Boolean = false) {
        if (text.isBlank()) {
            Toast.makeText(context, "No text to share", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, "Extracted Text - $docTitle")
                putExtra(Intent.EXTRA_TEXT, "*$docTitle*\n\n$text\n\n_Scanned via ScanPro AI_")
            }

            if (targetWhatsApp) {
                intent.setPackage("com.whatsapp")
            }

            val chooser = Intent.createChooser(intent, "Share Text via")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: Exception) {
            if (targetWhatsApp) {
                shareOcrText(context, docTitle, text, targetWhatsApp = false)
            } else {
                Toast.makeText(context, "Share failed: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadBitmapFromUri(context: Context, uriString: String): Bitmap? {
        return try {
            val uri = Uri.parse(uriString)
            val inputStream = context.contentResolver.openInputStream(uri)
            BitmapFactory.decodeStream(inputStream)
        } catch (e: Exception) {
            Log.e(LOG_TAG, "Bitmap load failed for $uriString", e)
            null
        }
    }
}
