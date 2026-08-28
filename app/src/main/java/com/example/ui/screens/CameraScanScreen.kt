package com.example.ui.screens

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.model.ScanMode
import com.example.ui.components.CameraPreviewComponent
import com.example.ui.theme.WarmOrangeSecondary
import com.example.ui.viewmodel.ScanViewModel

@Composable
fun CameraScanScreen(
    viewModel: ScanViewModel,
    onBack: () -> Unit,
    onProceedToEdit: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val capturedPages by viewModel.capturedPages.collectAsState()
    val scanMode by viewModel.scanMode.collectAsState()
    val flashEnabled by viewModel.flashEnabled.collectAsState()
    val gridEnabled by viewModel.gridEnabled.collectAsState()

    // Sample document feed options for camera simulator
    val sampleImageUris = remember {
        listOf(
            "android.resource://" + context.packageName + "/drawable/img_sample_document_1786037785507",
            "android.resource://" + context.packageName + "/drawable/img_sample_receipt_1786037765469",
            "android.resource://" + context.packageName + "/drawable/img_sample_id_card_1786037776617"
        )
    }
    var currentSampleIndex by remember { mutableStateOf(0) }
    var cameraShutterAction by remember { mutableStateOf<(() -> Unit)?>(null) }

    val galleryPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.capturePhoto(context, uri.toString())
            if (scanMode == ScanMode.SINGLE && capturedPages.isEmpty()) {
                onProceedToEdit()
            }
        }
    }

    // Edge Detection Bracket pulse & Scanning line animation
    val infiniteTransition = rememberInfiniteTransition(label = "scanAnimation")
    val alphaAnim by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bracketAlpha"
    )
    val scanYProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scanLineY"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // CameraX Live Preview Component
        CameraPreviewComponent(
            modifier = Modifier.fillMaxSize(),
            flashEnabled = flashEnabled,
            onImageCaptured = { savedUri ->
                viewModel.capturePhoto(context, savedUri.toString())
                if (scanMode == ScanMode.SINGLE && capturedPages.size >= 1) {
                    onProceedToEdit()
                }
            },
            onCameraCaptureReady = { shutterLambda ->
                cameraShutterAction = shutterLambda
            },
            fallbackSampleView = {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(sampleImageUris[currentSampleIndex % sampleImageUris.size])
                        .crossfade(true)
                        .build(),
                    contentDescription = "Camera Viewfinder",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        )

        // Grid Lines Overlay
        if (gridEnabled) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val gridColor = Color.White.copy(alpha = 0.25f)

                // Vertical lines
                drawLine(gridColor, start = Offset(w / 3, 0f), end = Offset(w / 3, h), strokeWidth = 1f)
                drawLine(gridColor, start = Offset(2 * w / 3, 0f), end = Offset(2 * w / 3, h), strokeWidth = 1f)

                // Horizontal lines
                drawLine(gridColor, start = Offset(0f, h / 3), end = Offset(w, h / 3), strokeWidth = 1f)
                drawLine(gridColor, start = Offset(0f, 2 * h / 3), end = Offset(w, 2 * h / 3), strokeWidth = 1f)
            }
        }

        // Auto Edge-Detection Animated Corner Brackets Overlay
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val padX = w * 0.12f
            val padY = h * 0.22f

            val left = padX
            val right = w - padX
            val top = padY
            val bottom = h - padY
            val bracketLen = 50f
            val strokeW = 8f
            val bracketColor = WarmOrangeSecondary.copy(alpha = alphaAnim)

            // Top-Left Corner
            val pathTL = Path().apply {
                moveTo(left, top + bracketLen)
                lineTo(left, top)
                lineTo(left + bracketLen, top)
            }
            drawPath(pathTL, bracketColor, style = Stroke(strokeW))

            // Top-Right Corner
            val pathTR = Path().apply {
                moveTo(right - bracketLen, top)
                lineTo(right, top)
                lineTo(right, top + bracketLen)
            }
            drawPath(pathTR, bracketColor, style = Stroke(strokeW))

            // Bottom-Right Corner
            val pathBR = Path().apply {
                moveTo(right, bottom - bracketLen)
                lineTo(right, bottom)
                lineTo(right - bracketLen, bottom)
            }
            drawPath(pathBR, bracketColor, style = Stroke(strokeW))

            // Bottom-Left Corner
            val pathBL = Path().apply {
                moveTo(left + bracketLen, bottom)
                lineTo(left, bottom)
                lineTo(left, bottom - bracketLen)
            }
            drawPath(pathBL, bracketColor, style = Stroke(strokeW))

            // Animated Laser Beam Scanning Line Effect (Top to Bottom laser sweep)
            val scanY = top + (bottom - top) * scanYProgress
            val laserColor = WarmOrangeSecondary
            val trailHeight = 70f
            val trailTop = (scanY - trailHeight).coerceAtLeast(top)

            if (scanY > top) {
                val trailBrush = Brush.verticalGradient(
                    colors = listOf(
                        laserColor.copy(alpha = 0.0f),
                        laserColor.copy(alpha = 0.15f),
                        laserColor.copy(alpha = 0.45f)
                    ),
                    startY = trailTop,
                    endY = scanY
                )
                drawRect(
                    brush = trailBrush,
                    topLeft = Offset(left, trailTop),
                    size = Size(right - left, scanY - trailTop)
                )
            }

            // Outer Glow Laser Line
            drawLine(
                color = laserColor.copy(alpha = 0.9f),
                start = Offset(left, scanY),
                end = Offset(right, scanY),
                strokeWidth = 6f
            )

            // Inner Bright White Core Line
            drawLine(
                color = Color.White,
                start = Offset(left + 12f, scanY),
                end = Offset(right - 12f, scanY),
                strokeWidth = 2.5f
            )
        }

        // Auto-Detect Pill Banner
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color.Black.copy(alpha = 0.65f),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 70.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(WarmOrangeSecondary, CircleShape)
                ) { }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Document Detected • Ready to Capture",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Top Toolbar (Back, Flash, Grid)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, top = 16.dp, end = 16.dp)
                .align(Alignment.TopCenter)
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                    .testTag("camera_back_btn")
            ) {
                Icon(Icons.Filled.Close, contentDescription = "Close", tint = Color.White)
            }

            Row {
                IconButton(
                    onClick = { viewModel.gridEnabled.value = !gridEnabled },
                    modifier = Modifier.background(Color.Black.copy(alpha = 0.5f), CircleShape)
                ) {
                    Icon(
                        Icons.Filled.GridOn,
                        contentDescription = "Grid",
                        tint = if (gridEnabled) WarmOrangeSecondary else Color.White
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                IconButton(
                    onClick = { viewModel.flashEnabled.value = !flashEnabled },
                    modifier = Modifier.background(Color.Black.copy(alpha = 0.5f), CircleShape)
                ) {
                    Icon(
                        if (flashEnabled) Icons.Filled.FlashOn else Icons.Filled.FlashOff,
                        contentDescription = "Flash",
                        tint = if (flashEnabled) WarmOrangeSecondary else Color.White
                    )
                }
            }
        }

        // Bottom Controls Container
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(Color.Black.copy(alpha = 0.85f))
                .padding(bottom = 24.dp)
        ) {
            // Mode Selector Strip
            LazyRow(
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
            ) {
                items(ScanMode.entries.size) { idx ->
                    val mode = ScanMode.entries[idx]
                    val isSelected = scanMode == mode
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 12.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { viewModel.scanMode.value = mode }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = mode.title,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) WarmOrangeSecondary else Color.LightGray
                        )
                    }
                }
            }

            // Filmstrip of captured pages if in Batch / multi-page mode
            if (capturedPages.isNotEmpty()) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                ) {
                    itemsIndexed(capturedPages) { index, page ->
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .border(1.dp, WarmOrangeSecondary, RoundedCornerShape(8.dp))
                        ) {
                            AsyncImage(
                                model = page.imageUri,
                                contentDescription = "Page #${index + 1}",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            Surface(
                                color = Color.Black.copy(alpha = 0.7f),
                                modifier = Modifier.align(Alignment.TopEnd)
                            ) {
                                Text(
                                    text = "${index + 1}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Shutter Row: Gallery Import | Shutter Button | Done Button
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            ) {
                // Gallery Import Button
                IconButton(
                    onClick = {
                        try {
                            galleryPickerLauncher.launch("image/*")
                        } catch (e: Exception) {
                            val sampleUri = sampleImageUris[currentSampleIndex % sampleImageUris.size]
                            currentSampleIndex++
                            viewModel.capturePhoto(context, sampleUri)
                        }
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .background(Color.White.copy(alpha = 0.15f), CircleShape)
                        .testTag("gallery_import_btn")
                ) {
                    Icon(Icons.Filled.PhotoLibrary, contentDescription = "Gallery", tint = Color.White)
                }

                // Shutter Button with Warm Orange Ring
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .border(4.dp, WarmOrangeSecondary, CircleShape)
                        .clickable {
                            if (cameraShutterAction != null) {
                                cameraShutterAction?.invoke()
                            } else {
                                val sampleUri = sampleImageUris[currentSampleIndex % sampleImageUris.size]
                                currentSampleIndex++
                                viewModel.capturePhoto(context, sampleUri)

                                if (scanMode == ScanMode.SINGLE && capturedPages.size >= 1) {
                                    onProceedToEdit()
                                }
                            }
                        }
                        .testTag("shutter_btn"),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .background(WarmOrangeSecondary, CircleShape)
                    )
                }

                // Done / Proceed CTA Button with page count badge
                Box {
                    IconButton(
                        onClick = {
                            if (capturedPages.isNotEmpty()) {
                                onProceedToEdit()
                            } else {
                                // Default capture 1 page if empty
                                val sampleUri = sampleImageUris[0]
                                viewModel.capturePhoto(context, sampleUri)
                                onProceedToEdit()
                            }
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .background(
                                if (capturedPages.isNotEmpty()) WarmOrangeSecondary else Color.White.copy(alpha = 0.15f),
                                CircleShape
                            )
                            .testTag("scan_done_btn")
                    ) {
                        Icon(Icons.Filled.Check, contentDescription = "Done", tint = Color.White)
                    }

                    if (capturedPages.isNotEmpty()) {
                        Badge(
                            containerColor = Color.Red,
                            contentColor = Color.White,
                            modifier = Modifier.align(Alignment.TopEnd)
                        ) {
                            Text("${capturedPages.size}")
                        }
                    }
                }
            }
        }
    }
}
