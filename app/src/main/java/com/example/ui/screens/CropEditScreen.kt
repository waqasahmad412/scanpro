package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Filter
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.model.ScanFilter
import com.example.ui.components.InteractiveCropCanvas
import com.example.ui.theme.IndigoPrimary
import com.example.ui.theme.WarmOrangeSecondary
import com.example.ui.viewmodel.ScanViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CropEditScreen(
    viewModel: ScanViewModel,
    onBack: () -> Unit,
    onProceedToSave: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val pages by viewModel.capturedPages.collectAsState()
    val pageIdx by viewModel.selectedCapturedPageIndex.collectAsState()
    val currentPage = pages.getOrNull(pageIdx) ?: pages.firstOrNull()

    var selectedTab by remember { mutableIntStateOf(0) } // 0: Crop/Rotate, 1: Filter, 2: Enhance Slider
    var splitPosition by remember { mutableFloatStateOf(0.5f) } // Before/After split position
    var isAutoEnhanceOn by remember { mutableStateOf(true) }
    var brightnessValue by remember { mutableFloatStateOf(0.1f) }
    var contrastValue by remember { mutableFloatStateOf(0.2f) }

    var cornerTL by remember(currentPage?.id) { mutableStateOf(currentPage?.cornerTL ?: (0.05f to 0.05f)) }
    var cornerTR by remember(currentPage?.id) { mutableStateOf(currentPage?.cornerTR ?: (0.95f to 0.05f)) }
    var cornerBR by remember(currentPage?.id) { mutableStateOf(currentPage?.cornerBR ?: (0.95f to 0.95f)) }
    var cornerBL by remember(currentPage?.id) { mutableStateOf(currentPage?.cornerBL ?: (0.05f to 0.95f)) }
    var isCropModified by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit & Enhance Page", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.Check, contentDescription = "Back", tint = IndigoPrimary)
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.rotatePage(pageIdx) }) {
                        Icon(Icons.Filled.RotateRight, contentDescription = "Rotate", tint = IndigoPrimary)
                    }
                    Button(
                        onClick = {
                            if (isCropModified) {
                                viewModel.cropPage(context, pageIdx, cornerTL, cornerTR, cornerBR, cornerBL)
                                isCropModified = false
                            }
                            onProceedToSave()
                        },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = WarmOrangeSecondary),
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .testTag("crop_save_next_btn")
                    ) {
                        Text("Save & Export", color = Color.White, fontWeight = FontWeight.Bold)
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
            // Main Image Editor Canvas Preview
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(Color(0xFF1E1E22)),
                contentAlignment = Alignment.Center
            ) {
                if (currentPage != null) {
                    // Page Image
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(currentPage.imageUri)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Editing Image",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .rotate(currentPage.rotation)
                    )

                    // Tab 0: Draggable Crop Corners Overlay
                    if (selectedTab == 0) {
                        InteractiveCropCanvas(
                            initialTL = cornerTL,
                            initialTR = cornerTR,
                            initialBR = cornerBR,
                            initialBL = cornerBL,
                            onCornersChanged = { tl, tr, br, bl ->
                                cornerTL = tl
                                cornerTR = tr
                                cornerBR = br
                                cornerBL = bl
                                isCropModified = true
                            }
                        )
                    }

                    // Tab 2: Before / After Split Slider Preview
                    if (selectedTab == 2) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .width(3.dp)
                                .background(WarmOrangeSecondary)
                                .align(Alignment.CenterStart)
                        )
                        Surface(
                            shape = CircleShape,
                            color = WarmOrangeSecondary,
                            modifier = Modifier.align(Alignment.Center)
                        ) {
                            Text(
                                "Split Preview",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            // Controls Mode Selector Tabs
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = IndigoPrimary
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Crop & Rotate", fontWeight = FontWeight.SemiBold) },
                    icon = { Icon(Icons.Filled.Crop, contentDescription = null) },
                    modifier = Modifier.testTag("tab_crop")
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Filters", fontWeight = FontWeight.SemiBold) },
                    icon = { Icon(Icons.Filled.Filter, contentDescription = null) },
                    modifier = Modifier.testTag("tab_filters")
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("Enhance", fontWeight = FontWeight.SemiBold) },
                    icon = { Icon(Icons.Filled.Tune, contentDescription = null) },
                    modifier = Modifier.testTag("tab_enhance")
                )
            }

            // Tab Content Control Panels
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 4.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    when (selectedTab) {
                        // 0: Crop & Rotate Controls
                        0 -> {
                            Column {
                                Text(
                                    "Drag corner handles to adjust edges, then tap Apply Crop.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Button(
                                        onClick = {
                                            viewModel.cropPage(context, pageIdx, cornerTL, cornerTR, cornerBR, cornerBL)
                                            isCropModified = false
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = WarmOrangeSecondary),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier
                                            .weight(1.2f)
                                            .testTag("apply_crop_btn")
                                    ) {
                                        Icon(Icons.Filled.Crop, contentDescription = null, tint = Color.White)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Apply Crop", color = Color.White, fontWeight = FontWeight.Bold)
                                    }

                                    Button(
                                        onClick = {
                                            cornerTL = 0.05f to 0.05f
                                            cornerTR = 0.95f to 0.05f
                                            cornerBR = 0.95f to 0.95f
                                            cornerBL = 0.05f to 0.95f
                                            isCropModified = false
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier
                                            .weight(0.9f)
                                            .testTag("reset_crop_btn")
                                    ) {
                                        Text("Reset", color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.SemiBold)
                                    }

                                    Button(
                                        onClick = { viewModel.rotatePage(pageIdx) },
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier
                                            .weight(0.9f)
                                            .testTag("rotate_btn")
                                    ) {
                                        Icon(Icons.Filled.RotateRight, contentDescription = null, tint = IndigoPrimary)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("90°", color = IndigoPrimary, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        // 1: Filter Carousel
                        1 -> {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(ScanFilter.entries) { filter ->
                                    val isSelected = currentPage?.filterType == filter
                                    Card(
                                        shape = RoundedCornerShape(12.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (isSelected) IndigoPrimary else MaterialTheme.colorScheme.surfaceVariant
                                        ),
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable { viewModel.updatePageFilter(context, pageIdx, filter) }
                                            .testTag("filter_${filter.name}")
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                                        ) {
                                            Text(
                                                text = filter.displayName,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // 2: Auto Enhance & Sliders
                        2 -> {
                            Column {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = WarmOrangeSecondary)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Auto Magic Enhance", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    }
                                    Switch(
                                        checked = isAutoEnhanceOn,
                                        onCheckedChange = { isAutoEnhanceOn = it },
                                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = WarmOrangeSecondary)
                                    )
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("Brightness", style = MaterialTheme.typography.labelMedium, modifier = Modifier.width(80.dp))
                                    Slider(
                                        value = brightnessValue,
                                        onValueChange = {
                                            brightnessValue = it
                                            viewModel.updatePageEnhance(context, pageIdx, brightnessValue, contrastValue)
                                        },
                                        valueRange = -0.5f..0.5f,
                                        colors = SliderDefaults.colors(thumbColor = IndigoPrimary, activeTrackColor = IndigoPrimary),
                                        modifier = Modifier.weight(1f)
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("Contrast", style = MaterialTheme.typography.labelMedium, modifier = Modifier.width(80.dp))
                                    Slider(
                                        value = contrastValue,
                                        onValueChange = {
                                            contrastValue = it
                                            viewModel.updatePageEnhance(context, pageIdx, brightnessValue, contrastValue)
                                        },
                                        valueRange = -0.5f..0.5f,
                                        colors = SliderDefaults.colors(thumbColor = IndigoPrimary, activeTrackColor = IndigoPrimary),
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
