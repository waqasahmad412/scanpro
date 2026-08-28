package com.example.ui.components

import android.graphics.Bitmap
import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.IndigoPrimary
import com.example.ui.theme.WarmOrangeSecondary

data class StrokeData(
    val points: List<Offset>,
    val color: Color,
    val strokeWidth: Float
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignatureModal(
    onDismiss: () -> Unit,
    onSignatureSaved: (Bitmap) -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var selectedTab by remember { mutableStateOf(0) } // 0 = Draw, 1 = Type
    val strokes = remember { mutableStateListOf<StrokeData>() }
    var currentPoints = remember { mutableStateListOf<Offset>() }

    var selectedPenColor by remember { mutableStateOf(Color.Black) }
    var selectedStrokeWidth by remember { mutableStateOf(8f) }
    var typedName by remember { mutableStateOf("") }
    var canvasSize by remember { mutableStateOf(Pair(700, 350)) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        modifier = modifier.testTag("signature_modal")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Draw, contentDescription = null, tint = IndigoPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "E-Signature Tool",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (selectedTab == 0 && (strokes.isNotEmpty() || currentPoints.isNotEmpty())) {
                    Row {
                        IconButton(
                            onClick = {
                                if (currentPoints.isNotEmpty()) {
                                    currentPoints.clear()
                                } else if (strokes.isNotEmpty()) {
                                    strokes.removeAt(strokes.size - 1)
                                }
                            }
                        ) {
                            Icon(Icons.Filled.Undo, contentDescription = "Undo", tint = IndigoPrimary)
                        }
                        IconButton(
                            onClick = {
                                strokes.clear()
                                currentPoints.clear()
                            }
                        ) {
                            Icon(Icons.Filled.Clear, contentDescription = "Clear Canvas", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Tab Selector (Draw / Type)
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.clip(RoundedCornerShape(12.dp)),
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = IndigoPrimary
                    )
                }
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Draw, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Draw Signature", fontWeight = FontWeight.Bold)
                        }
                    },
                    selectedContentColor = IndigoPrimary,
                    unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.TextFields, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Type Name", fontWeight = FontWeight.Bold)
                        }
                    },
                    selectedContentColor = IndigoPrimary,
                    unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (selectedTab == 0) {
                // Drawing Canvas
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White)
                        .border(1.5.dp, IndigoPrimary.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragStart = { offset ->
                                    currentPoints.add(offset)
                                },
                                onDrag = { change, _ ->
                                    change.consume()
                                    currentPoints.add(change.position)
                                },
                                onDragEnd = {
                                    if (currentPoints.isNotEmpty()) {
                                        strokes.add(StrokeData(currentPoints.toList(), selectedPenColor, selectedStrokeWidth))
                                        currentPoints.clear()
                                    }
                                },
                                onDragCancel = {
                                    if (currentPoints.isNotEmpty()) {
                                        strokes.add(StrokeData(currentPoints.toList(), selectedPenColor, selectedStrokeWidth))
                                        currentPoints.clear()
                                    }
                                }
                            )
                        }
                        .testTag("signature_canvas")
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        canvasSize = Pair(
                            size.width.toInt().coerceAtLeast(200),
                            size.height.toInt().coerceAtLeast(100)
                        )

                        // Render finished strokes
                        strokes.forEach { stroke ->
                            if (stroke.points.size > 1) {
                                val path = Path().apply {
                                    moveTo(stroke.points[0].x, stroke.points[0].y)
                                    for (i in 1 until stroke.points.size) {
                                        lineTo(stroke.points[i].x, stroke.points[i].y)
                                    }
                                }
                                drawPath(
                                    path = path,
                                    color = stroke.color,
                                    style = Stroke(width = stroke.strokeWidth)
                                )
                            }
                        }

                        // Render active drawing stroke
                        if (currentPoints.size > 1) {
                            val activePath = Path().apply {
                                moveTo(currentPoints[0].x, currentPoints[0].y)
                                for (i in 1 until currentPoints.size) {
                                    lineTo(currentPoints[i].x, currentPoints[i].y)
                                }
                            }
                            drawPath(
                                path = activePath,
                                color = selectedPenColor,
                                style = Stroke(width = selectedStrokeWidth)
                            )
                        }
                    }

                    if (strokes.isEmpty() && currentPoints.isEmpty()) {
                        Text(
                            text = "✍️ Sign here with your finger",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.Gray.copy(alpha = 0.5f),
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Color & Pen Thickness controls
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Pen Color
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Color:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        listOf(Color.Black, Color(0xFF002288), Color(0xFF880000), Color(0xFF006622)).forEach { color ->
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .background(color, CircleShape)
                                    .border(
                                        width = if (selectedPenColor == color) 3.dp else 0.dp,
                                        color = WarmOrangeSecondary,
                                        shape = CircleShape
                                    )
                                    .clickable { selectedPenColor = color }
                            )
                        }
                    }

                    // Stroke Thickness
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("Thickness:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        listOf(4f to "Thin", 8f to "Med", 14f to "Thick").forEach { (width, label) ->
                            val isSelected = selectedStrokeWidth == width
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) IndigoPrimary else MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable { selectedStrokeWidth = width }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            } else {
                // Type Signature UI
                Column(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = typedName,
                        onValueChange = { typedName = it },
                        label = { Text("Enter Your Name") },
                        placeholder = { Text("e.g. Waqas Ahmad") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = IndigoPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Text("Signature Style Preview:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White)
                            .border(1.5.dp, IndigoPrimary.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                            .padding(16.dp)
                    ) {
                        Text(
                            text = if (typedName.isNotBlank()) typedName else "Your Signature Here",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = selectedPenColor,
                            modifier = Modifier.padding(8.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Ink Color:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        listOf(Color.Black, Color(0xFF002288), Color(0xFF880000), Color(0xFF006622)).forEach { color ->
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .background(color, CircleShape)
                                    .border(
                                        width = if (selectedPenColor == color) 3.dp else 0.dp,
                                        color = WarmOrangeSecondary,
                                        shape = CircleShape
                                    )
                                    .clickable { selectedPenColor = color }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cancel")
                }

                Button(
                    onClick = {
                        val bitmap = if (selectedTab == 0) {
                            createDrawnSignatureBitmap(strokes, canvasSize.first, canvasSize.second)
                        } else {
                            createTypedSignatureBitmap(typedName.ifBlank { "Signed" }, selectedPenColor)
                        }

                        val cropped = cropTransparentBitmap(bitmap)
                        onSignatureSaved(cropped)
                        onDismiss()
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
                    enabled = if (selectedTab == 0) strokes.isNotEmpty() else typedName.isNotBlank(),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("apply_signature_btn")
                ) {
                    Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Apply Signature", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

private fun createDrawnSignatureBitmap(
    strokes: List<StrokeData>,
    width: Int,
    height: Int
): Bitmap {
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)

    strokes.forEach { stroke ->
        if (stroke.points.size > 1) {
            val paint = Paint().apply {
                isAntiAlias = true
                color = stroke.color.toArgb()
                style = Paint.Style.STROKE
                strokeWidth = stroke.strokeWidth
                strokeCap = Paint.Cap.ROUND
                strokeJoin = Paint.Join.ROUND
            }

            val path = android.graphics.Path().apply {
                moveTo(stroke.points[0].x, stroke.points[0].y)
                for (i in 1 until stroke.points.size) {
                    lineTo(stroke.points[i].x, stroke.points[i].y)
                }
            }
            canvas.drawPath(path, paint)
        }
    }

    return bitmap
}

private fun createTypedSignatureBitmap(
    text: String,
    color: Color
): Bitmap {
    val paint = Paint().apply {
        isAntiAlias = true
        this.color = color.toArgb()
        textSize = 72f
        typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD_ITALIC)
        textAlign = Paint.Align.LEFT
    }

    val bounds = android.graphics.Rect()
    paint.getTextBounds(text, 0, text.length, bounds)

    val width = (bounds.width() + 80).coerceAtLeast(300)
    val height = (bounds.height() + 80).coerceAtLeast(120)

    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)

    val x = 40f
    val y = (height / 2f) + (bounds.height() / 2f) - bounds.bottom
    canvas.drawText(text, x, y, paint)

    return bitmap
}

private fun cropTransparentBitmap(srcBmp: Bitmap): Bitmap {
    val w = srcBmp.width
    val h = srcBmp.height
    var minX = w
    var minY = h
    var maxX = -1
    var maxY = -1

    val pixels = IntArray(w * h)
    srcBmp.getPixels(pixels, 0, w, 0, 0, w, h)

    for (y in 0 until h) {
        for (x in 0 until w) {
            val alpha = (pixels[y * w + x] shr 24) and 0xFF
            if (alpha > 15) {
                if (x < minX) minX = x
                if (x > maxX) maxX = x
                if (y < minY) minY = y
                if (y > maxY) maxY = y
            }
        }
    }

    if (maxX < minX || maxY < minY) return srcBmp

    val padding = 16
    val startX = (minX - padding).coerceAtLeast(0)
    val startY = (minY - padding).coerceAtLeast(0)
    val endX = (maxX + padding).coerceAtMost(w - 1)
    val endY = (maxY + padding).coerceAtMost(h - 1)

    val cropW = endX - startX + 1
    val cropH = endY - startY + 1

    return try {
        Bitmap.createBitmap(srcBmp, startX, startY, cropW, cropH)
    } catch (e: Exception) {
        srcBmp
    }
}

