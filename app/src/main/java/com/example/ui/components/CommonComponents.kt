package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContactPage
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarOutline
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.model.DocumentEntity
import com.example.data.model.FolderEntity
import com.example.ui.theme.IndigoPrimary
import com.example.ui.theme.WarmOrangeSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ScanBottomBar(
    currentRoute: String,
    onNavigate: (String) -> Unit,
    onOpenScan: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        com.example.util.BannerAdView()

        NavigationBar(
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            NavigationBarItem(
                selected = currentRoute == "home",
                onClick = { onNavigate("home") },
                icon = {
                    Icon(
                        if (currentRoute == "home") Icons.Filled.Home else Icons.Outlined.Home,
                        contentDescription = "Home"
                    )
                },
                label = { Text("Home", style = MaterialTheme.typography.labelSmall) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = IndigoPrimary,
                    selectedTextColor = IndigoPrimary,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer
                ),
                modifier = Modifier.testTag("nav_home")
            )

            NavigationBarItem(
                selected = currentRoute == "folders",
                onClick = { onNavigate("folders") },
                icon = {
                    Icon(
                        if (currentRoute == "folders") Icons.Filled.Folder else Icons.Outlined.Folder,
                        contentDescription = "Folders"
                    )
                },
                label = { Text("Folders", style = MaterialTheme.typography.labelSmall) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = IndigoPrimary,
                    selectedTextColor = IndigoPrimary,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer
                ),
                modifier = Modifier.testTag("nav_folders")
            )

            // Center Spacer for elevated Scan button
            NavigationBarItem(
                selected = false,
                onClick = onOpenScan,
                icon = {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .shadow(8.dp, CircleShape)
                            .background(WarmOrangeSecondary, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.Camera,
                            contentDescription = "New Scan",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                },
                label = { Text("Scan", style = MaterialTheme.typography.labelSmall, color = WarmOrangeSecondary, fontWeight = FontWeight.Bold) },
                modifier = Modifier.testTag("nav_scan_fab")
            )

            NavigationBarItem(
                selected = currentRoute == "tools",
                onClick = { onNavigate("tools") },
                icon = {
                    Icon(
                        if (currentRoute == "tools") Icons.Filled.Tune else Icons.Outlined.Tune,
                        contentDescription = "Tools"
                    )
                },
                label = { Text("Tools", style = MaterialTheme.typography.labelSmall) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = IndigoPrimary,
                    selectedTextColor = IndigoPrimary,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer
                ),
                modifier = Modifier.testTag("nav_tools")
            )

            NavigationBarItem(
                selected = currentRoute == "settings",
                onClick = { onNavigate("settings") },
                icon = {
                    Icon(
                        if (currentRoute == "settings") Icons.Filled.Settings else Icons.Outlined.Settings,
                        contentDescription = "Settings"
                    )
                },
                label = { Text("Settings", style = MaterialTheme.typography.labelSmall) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = IndigoPrimary,
                    selectedTextColor = IndigoPrimary,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer
                ),
                modifier = Modifier.testTag("nav_settings")
            )
        }
    }
}

@Composable
fun DocumentCard(
    document: DocumentEntity,
    onClick: () -> Unit,
    onFavoriteToggle: () -> Unit,
    onDelete: () -> Unit,
    onShare: (() -> Unit)? = null,
    isSelected: Boolean = false,
    isSelectionMode: Boolean = false,
    onSelectToggle: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }
    val formattedDate = remember(document.modifiedAt) {
        SimpleDateFormat("MMM dd, yyyy • HH:mm", Locale.getDefault()).format(Date(document.modifiedAt))
    }

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable {
                if (isSelectionMode) onSelectToggle() else onClick()
            }
            .border(
                width = if (isSelected) 2.dp else 0.5.dp,
                color = if (isSelected) IndigoPrimary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                shape = RoundedCornerShape(18.dp)
            )
            .testTag("doc_card_${document.id}")
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.3f)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(document.thumbnailUri)
                        .crossfade(true)
                        .build(),
                    contentDescription = document.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // Page Count Badge
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color.Black.copy(alpha = 0.7f),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            Icons.Filled.Description,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${document.pageCount} ${if (document.pageCount == 1) "Page" else "Pages"}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White
                        )
                    }
                }

                // Password Lock Badge if encrypted
                if (document.passwordLock != null) {
                    Surface(
                        shape = CircleShape,
                        color = Color.Black.copy(alpha = 0.7f),
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp)
                    ) {
                        Icon(
                            Icons.Filled.Lock,
                            contentDescription = "Encrypted",
                            tint = WarmOrangeSecondary,
                            modifier = Modifier
                                .padding(6.dp)
                                .size(14.dp)
                        )
                    }
                }

                // Selection Checkmark Overlay
                if (isSelectionMode) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp)
                            .size(24.dp)
                            .background(
                                if (isSelected) IndigoPrimary else Color.White.copy(alpha = 0.8f),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            // Text Info Footer
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = document.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(Icons.Filled.MoreVert, contentDescription = "Menu", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        if (onShare != null) {
                            DropdownMenuItem(
                                text = { Text("Share Document") },
                                leadingIcon = { Icon(Icons.Outlined.Share, contentDescription = null, tint = IndigoPrimary) },
                                onClick = {
                                    showMenu = false
                                    onShare()
                                }
                            )
                        }
                        DropdownMenuItem(
                            text = { Text(if (document.isFavorite) "Remove Favorite" else "Add Favorite") },
                            leadingIcon = {
                                Icon(
                                    if (document.isFavorite) Icons.Filled.Star else Icons.Filled.StarOutline,
                                    contentDescription = null,
                                    tint = WarmOrangeSecondary
                                )
                            },
                            onClick = {
                                showMenu = false
                                onFavoriteToggle()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete") },
                            leadingIcon = { Icon(Icons.Outlined.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                            onClick = {
                                showMenu = false
                                onDelete()
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Folder pill
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = document.folderName,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    Text(
                        text = formattedDate,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}

@Composable
fun FolderCard(
    folder: FolderEntity,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val icon = remember(folder.iconName) {
        when (folder.iconName) {
            "receipt" -> Icons.Filled.Receipt
            "badge" -> Icons.Filled.Badge
            "contact_page" -> Icons.Filled.ContactPage
            "notes" -> Icons.Filled.Notes
            else -> Icons.Filled.Folder
        }
    }

    val iconColor = remember(folder.colorHex) {
        try {
            Color(android.graphics.Color.parseColor(folder.colorHex))
        } catch (e: Exception) {
            IndigoPrimary
        }
    }

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .border(1.dp, iconColor.copy(alpha = 0.3f), RoundedCornerShape(18.dp))
            .testTag("folder_card_${folder.name}")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(14.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = iconColor.copy(alpha = 0.15f),
                modifier = Modifier.size(46.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = folder.name, tint = iconColor, modifier = Modifier.size(26.dp))
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = folder.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = iconColor.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = "${folder.docCount} ${if (folder.docCount == 1) "File" else "Files"}",
                        style = MaterialTheme.typography.labelSmall,
                        color = iconColor,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}


@Composable
fun EmptyStateIllustration(
    title: String = "No Documents Yet",
    subtitle: String = "Tap the warm orange camera button below to scan your first receipt, ID card, or document.",
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp)
    ) {
        Box(
            modifier = Modifier
                .size(100.dp)
                .background(IndigoPrimary.copy(alpha = 0.1f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Outlined.Description,
                contentDescription = null,
                tint = IndigoPrimary,
                modifier = Modifier.size(48.dp)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth(0.85f),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

@Composable
fun InteractiveCropCanvas(
    modifier: Modifier = Modifier,
    initialTL: Pair<Float, Float> = 0.05f to 0.05f,
    initialTR: Pair<Float, Float> = 0.95f to 0.05f,
    initialBR: Pair<Float, Float> = 0.95f to 0.95f,
    initialBL: Pair<Float, Float> = 0.05f to 0.95f,
    onCornersChanged: (Pair<Float, Float>, Pair<Float, Float>, Pair<Float, Float>, Pair<Float, Float>) -> Unit = { _, _, _, _ -> }
) {
    var normTL by remember(initialTL) { mutableStateOf(initialTL) }
    var normTR by remember(initialTR) { mutableStateOf(initialTR) }
    var normBR by remember(initialBR) { mutableStateOf(initialBR) }
    var normBL by remember(initialBL) { mutableStateOf(initialBL) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    val w = size.width.toFloat().coerceAtLeast(1f)
                    val h = size.height.toFloat().coerceAtLeast(1f)

                    val pos = change.position
                    val pxTL = Offset(normTL.first * w, normTL.second * h)
                    val pxTR = Offset(normTR.first * w, normTR.second * h)
                    val pxBR = Offset(normBR.first * w, normBR.second * h)
                    val pxBL = Offset(normBL.first * w, normBL.second * h)

                    val dTL = (pos - pxTL).getDistance()
                    val dTR = (pos - pxTR).getDistance()
                    val dBR = (pos - pxBR).getDistance()
                    val dBL = (pos - pxBL).getDistance()

                    val minD = minOf(dTL, dTR, dBR, dBL)
                    val dx = dragAmount.x / w
                    val dy = dragAmount.y / h

                    when (minD) {
                        dTL -> normTL = (normTL.first + dx).coerceIn(0f, normTR.first - 0.05f) to (normTL.second + dy).coerceIn(0f, normBL.second - 0.05f)
                        dTR -> normTR = (normTR.first + dx).coerceIn(normTL.first + 0.05f, 1f) to (normTR.second + dy).coerceIn(0f, normBR.second - 0.05f)
                        dBR -> normBR = (normBR.first + dx).coerceIn(normBL.first + 0.05f, 1f) to (normBR.second + dy).coerceIn(normTR.second + 0.05f, 1f)
                        dBL -> normBL = (normBL.first + dx).coerceIn(0f, normBR.first - 0.05f) to (normBL.second + dy).coerceIn(normTL.second + 0.05f, 1f)
                    }

                    onCornersChanged(normTL, normTR, normBR, normBL)
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            val pxTL = Offset(normTL.first * w, normTL.second * h)
            val pxTR = Offset(normTR.first * w, normTR.second * h)
            val pxBR = Offset(normBR.first * w, normBR.second * h)
            val pxBL = Offset(normBL.first * w, normBL.second * h)

            // Crop Polygon Path
            val cropPath = Path().apply {
                moveTo(pxTL.x, pxTL.y)
                lineTo(pxTR.x, pxTR.y)
                lineTo(pxBR.x, pxBR.y)
                lineTo(pxBL.x, pxBL.y)
                close()
            }

            // Darkened Outer Mask Path
            val maskPath = Path().apply {
                fillType = androidx.compose.ui.graphics.PathFillType.EvenOdd
                addRect(androidx.compose.ui.geometry.Rect(0f, 0f, w, h))
                addPath(cropPath)
            }

            drawPath(path = maskPath, color = Color.Black.copy(alpha = 0.55f))

            // Dashed Border
            drawPath(
                path = cropPath,
                color = WarmOrangeSecondary,
                style = Stroke(
                    width = 4f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(16f, 12f), 0f)
                )
            )

            // Rule of thirds grid lines inside crop box
            val p13TLTR = Offset(pxTL.x + (pxTR.x - pxTL.x) / 3f, pxTL.y + (pxTR.y - pxTL.y) / 3f)
            val p13BLBR = Offset(pxBL.x + (pxBR.x - pxBL.x) / 3f, pxBL.y + (pxBR.y - pxBL.y) / 3f)
            drawLine(Color.White.copy(alpha = 0.4f), p13TLTR, p13BLBR, strokeWidth = 1.5f)

            val p23TLTR = Offset(pxTL.x + 2f * (pxTR.x - pxTL.x) / 3f, pxTL.y + 2f * (pxTR.y - pxTL.y) / 3f)
            val p23BLBR = Offset(pxBL.x + 2f * (pxBR.x - pxBL.x) / 3f, pxBL.y + 2f * (pxBR.y - pxBL.y) / 3f)
            drawLine(Color.White.copy(alpha = 0.4f), p23TLTR, p23BLBR, strokeWidth = 1.5f)

            val p13TLBL = Offset(pxTL.x + (pxBL.x - pxTL.x) / 3f, pxTL.y + (pxBL.y - pxTL.y) / 3f)
            val p13TRBR = Offset(pxTR.x + (pxBR.x - pxTR.x) / 3f, pxTR.y + (pxBR.y - pxTR.y) / 3f)
            drawLine(Color.White.copy(alpha = 0.4f), p13TLBL, p13TRBR, strokeWidth = 1.5f)

            val p23TLBL = Offset(pxTL.x + 2f * (pxBL.x - pxTL.x) / 3f, pxTL.y + 2f * (pxBL.y - pxTL.y) / 3f)
            val p23TRBR = Offset(pxTR.x + 2f * (pxBR.x - pxTR.x) / 3f, pxTR.y + 2f * (pxBR.y - pxTR.y) / 3f)
            drawLine(Color.White.copy(alpha = 0.4f), p23TLBL, p23TRBR, strokeWidth = 1.5f)

            // Corner Handles
            val handleRadius = 22f
            listOf(pxTL, pxTR, pxBR, pxBL).forEach { center ->
                drawCircle(color = WarmOrangeSecondary, radius = handleRadius, center = center)
                drawCircle(color = Color.White, radius = handleRadius - 6f, center = center)
                drawCircle(color = IndigoPrimary, radius = handleRadius - 12f, center = center)
            }
        }
    }
}
