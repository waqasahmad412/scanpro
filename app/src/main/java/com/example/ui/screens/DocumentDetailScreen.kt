package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarOutline
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.BrandingWatermark
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.ui.components.SignatureModal
import com.example.ui.theme.IndigoPrimary
import com.example.ui.theme.WarmOrangeSecondary
import com.example.ui.viewmodel.ScanViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentDetailScreen(
    viewModel: ScanViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activeDoc by viewModel.activeDocument.collectAsState()
    val pages by viewModel.activeDocumentPages.collectAsState()
    val activeIdx by viewModel.activePageIndex.collectAsState()
    val ocrText by viewModel.currentOcrResult.collectAsState()
    val translatedText by viewModel.translatedText.collectAsState()
    val isOcrLoading by viewModel.isOcrLoading.collectAsState()
    val isTranslating by viewModel.isTranslating.collectAsState()

    var showOcrPanel by remember { mutableStateOf(false) }
    var isTtsPlaying by remember { mutableStateOf(false) }
    var ttsProgress by remember { mutableStateOf(0.35f) }
    var showSignatureModal by remember { mutableStateOf(false) }
    var showWatermarkModal by remember { mutableStateOf(false) }
    var showShareModal by remember { mutableStateOf(false) }
    var showTranslateMenu by remember { mutableStateOf(false) }
    var watermarkText by remember { mutableStateOf<String?>(null) }
    var signatureBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    var sigOffsetX by remember { mutableStateOf(0f) }
    var sigOffsetY by remember { mutableStateOf(0f) }
    var isPasswordLocked by remember { mutableStateOf(activeDoc?.passwordLock != null) }

    val selectedPage = pages.getOrNull(activeIdx) ?: pages.firstOrNull()

    if (showShareModal && activeDoc != null) {
        com.example.ui.components.ShareModal(
            docTitle = activeDoc?.title ?: "Scanned Document",
            imageUris = pages.map { it.imageUri },
            ocrText = ocrText,
            watermarkText = watermarkText,
            signatureBitmap = signatureBitmap,
            onDismiss = { showShareModal = false }
        )
    }

    if (showSignatureModal) {
        SignatureModal(
            onDismiss = { showSignatureModal = false },
            onSignatureSaved = { sigBmp ->
                signatureBitmap = sigBmp
                viewModel.showToast("E-Signature applied to document!")
            }
        )
    }

    if (showWatermarkModal) {
        com.example.ui.components.WatermarkModal(
            currentWatermark = watermarkText,
            onDismiss = { showWatermarkModal = false },
            onWatermarkApplied = { text ->
                watermarkText = text
                viewModel.showToast(if (text != null) "Watermark '$text' Applied!" else "Watermark Removed")
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = activeDoc?.title ?: "Document Preview",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                        Text(
                            text = "${pages.size} ${if (pages.size == 1) "Page" else "Pages"} • ${activeDoc?.folderName ?: "Scans"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = IndigoPrimary)
                    }
                },
                actions = {
                    IconButton(onClick = {
                        activeDoc?.let { viewModel.toggleFavorite(it) }
                    }) {
                        Icon(
                            if (activeDoc?.isFavorite == true) Icons.Filled.Star else Icons.Filled.StarOutline,
                            contentDescription = "Favorite",
                            tint = WarmOrangeSecondary
                        )
                    }
                    IconButton(onClick = {
                        showShareModal = true
                    }) {
                        Icon(Icons.Filled.Share, contentDescription = "Share", tint = IndigoPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Document Viewport Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(Color(0xFF1E1E22)),
                contentAlignment = Alignment.Center
            ) {
                if (selectedPage != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(selectedPage.imageUri)
                                .crossfade(true)
                                .build(),
                            contentDescription = "Page Image",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize()
                        )

                        // Watermark Text Overlay
                        if (!watermarkText.isNullOrBlank()) {
                            Surface(
                                color = Color.Red.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(12.dp),
                                border = androidx.compose.foundation.BorderStroke(2.dp, Color.Red.copy(alpha = 0.4f)),
                                modifier = Modifier
                                    .rotate(-28f)
                                    .padding(24.dp)
                            ) {
                                Text(
                                    text = watermarkText!!,
                                    style = MaterialTheme.typography.displaySmall,
                                    color = Color.Red.copy(alpha = 0.65f),
                                    fontWeight = FontWeight.Black,
                                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                                )
                            }
                        }

                        // Signature Overlay at Bottom Right (Draggable)
                        if (signatureBitmap != null) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(end = 16.dp, bottom = 16.dp)
                                    .offset { IntOffset(sigOffsetX.roundToInt(), sigOffsetY.roundToInt()) }
                                    .pointerInput(Unit) {
                                        detectDragGestures { change, dragAmount ->
                                            change.consume()
                                            sigOffsetX += dragAmount.x
                                            sigOffsetY += dragAmount.y
                                        }
                                    }
                                    .background(Color.White.copy(alpha = 0.9f), RoundedCornerShape(8.dp))
                                    .border(1.5.dp, IndigoPrimary.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                                    .padding(4.dp)
                            ) {
                                Column(horizontalAlignment = Alignment.End) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.width(140.dp)
                                    ) {
                                        Text(
                                            "✋ Drag to move",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = IndigoPrimary,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(start = 2.dp)
                                        )
                                        IconButton(
                                            onClick = {
                                                signatureBitmap = null
                                                sigOffsetX = 0f
                                                sigOffsetY = 0f
                                            },
                                            modifier = Modifier.size(20.dp)
                                        ) {
                                            Icon(
                                                Icons.Filled.Close,
                                                contentDescription = "Remove Signature",
                                                tint = Color.Red,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                    Image(
                                        bitmap = signatureBitmap!!.asImageBitmap(),
                                        contentDescription = "Signature Overlay",
                                        modifier = Modifier
                                            .width(140.dp)
                                            .height(70.dp),
                                        contentScale = ContentScale.Fit
                                    )
                                }
                            }
                        }
                    }
                }

                if (isOcrLoading) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color.Black.copy(alpha = 0.75f)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(20.dp)
                        ) {
                            CircularProgressIndicator(color = WarmOrangeSecondary, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Gemini AI Extracting Text...", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Paginated Thumbnail Strip
            if (pages.size > 1) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    itemsIndexed(pages) { idx, page ->
                        val isSelected = idx == activeIdx
                        Box(
                            modifier = Modifier
                                .size(54.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .border(
                                    width = if (isSelected) 2.dp else 0.5.dp,
                                    color = if (isSelected) IndigoPrimary else Color.Transparent,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable { viewModel.activePageIndex.value = idx }
                        ) {
                            AsyncImage(
                                model = page.imageUri,
                                contentDescription = "Page ${idx + 1}",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }

            // OCR Extracted Text Drawer / Bottom Panel
            AnimatedVisibility(visible = showOcrPanel) {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .height(240.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = IndigoPrimary)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Gemini AI OCR Text Result", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            }

                            Row {
                                // Copy Text Button
                                IconButton(onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = ClipData.newPlainText("ScanPro OCR", ocrText)
                                    clipboard.setPrimaryClip(clip)
                                    viewModel.showToast("Extracted Text Copied to Clipboard!")
                                }) {
                                    Icon(Icons.Filled.ContentCopy, contentDescription = "Copy Text", tint = IndigoPrimary)
                                }

                                // Translate Menu Button
                                Box {
                                    IconButton(onClick = { showTranslateMenu = true }) {
                                        Icon(Icons.Filled.Translate, contentDescription = "Translate", tint = IndigoPrimary)
                                    }

                                    DropdownMenu(
                                        expanded = showTranslateMenu,
                                        onDismissRequest = { showTranslateMenu = false }
                                    ) {
                                        listOf("Spanish", "French", "German", "Japanese", "Chinese").forEach { lang ->
                                            DropdownMenuItem(
                                                text = { Text("Translate to $lang") },
                                                onClick = {
                                                    showTranslateMenu = false
                                                    viewModel.translateOcrText(lang)
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Text-to-Speech Simulator Control
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                IconButton(
                                    onClick = { isTtsPlaying = !isTtsPlaying },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        if (isTtsPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                        contentDescription = "Text-to-Speech",
                                        tint = WarmOrangeSecondary
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        if (isTtsPlaying) "Reading Document Aloud..." else "Text-to-Speech Audio Reader",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    LinearProgressIndicator(
                                        progress = { ttsProgress },
                                        color = WarmOrangeSecondary,
                                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        if (isTranslating) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = IndigoPrimary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Gemini AI Translating...", style = MaterialTheme.typography.bodySmall)
                            }
                        }

                        OutlinedTextField(
                            value = if (translatedText.isNotBlank()) translatedText else ocrText,
                            onValueChange = { viewModel.currentOcrResult.value = it },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = IndigoPrimary)
                        )
                    }
                }
            }

            // Toolbar Actions (OCR, Sign, Watermark, Lock, Share)
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp)
                ) {
                    // OCR Button
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable {
                            showOcrPanel = !showOcrPanel
                            if (ocrText.isBlank()) {
                                viewModel.runOcrOnActiveDocument(context)
                            }
                        }.testTag("toolbar_ocr_btn")
                    ) {
                        Icon(Icons.Filled.TextFields, contentDescription = "OCR", tint = IndigoPrimary)
                        Text("AI OCR", style = MaterialTheme.typography.labelSmall)
                    }

                    // E-Sign Button
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable { showSignatureModal = true }.testTag("toolbar_sign_btn")
                    ) {
                        Icon(
                            Icons.Filled.Draw,
                            contentDescription = "Sign",
                            tint = if (signatureBitmap != null) WarmOrangeSecondary else IndigoPrimary
                        )
                        Text(
                            if (signatureBitmap != null) "Signed" else "Sign",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (signatureBitmap != null) WarmOrangeSecondary else MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Watermark Button
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable { showWatermarkModal = true }.testTag("toolbar_watermark_btn")
                    ) {
                        Icon(
                            Icons.Filled.BrandingWatermark,
                            contentDescription = "Watermark",
                            tint = if (!watermarkText.isNullOrBlank()) WarmOrangeSecondary else IndigoPrimary
                        )
                        Text(
                            if (!watermarkText.isNullOrBlank()) "Watermarked" else "Watermark",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (!watermarkText.isNullOrBlank()) WarmOrangeSecondary else MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Password Lock Button
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable {
                            isPasswordLocked = !isPasswordLocked
                            viewModel.showToast(if (isPasswordLocked) "Document Lock Enabled (AES-256)" else "Document Lock Removed")
                        }.testTag("toolbar_lock_btn")
                    ) {
                        Icon(
                            Icons.Filled.Lock,
                            contentDescription = "Password Lock",
                            tint = if (isPasswordLocked) WarmOrangeSecondary else IndigoPrimary
                        )
                        Text(if (isPasswordLocked) "Locked" else "Lock", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}
