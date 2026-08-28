package com.example.ui.screens

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BrandingWatermark
import androidx.compose.material.icons.filled.Compress
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Merge
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.components.ScanBottomBar
import com.example.ui.theme.IndigoPrimary
import com.example.ui.theme.VividAmber
import com.example.ui.theme.VividCrimson
import com.example.ui.theme.VividEmerald
import com.example.ui.theme.VividPurple
import com.example.ui.theme.VividRose
import com.example.ui.theme.VividTeal
import com.example.ui.theme.WarmOrangeSecondary
import com.example.ui.viewmodel.ScanViewModel

data class ToolItem(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val tag: String,
    val accentColor: Color,
    val badgeText: String? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToolsScreen(
    viewModel: ScanViewModel,
    onNavigate: (String) -> Unit,
    onOpenScan: () -> Unit,
    modifier: Modifier = Modifier
) {
    val toolItems = listOf(
        ToolItem(
            title = "Batch Gemini AI OCR",
            description = "Extract text from multiple scanned pages automatically using Google Gemini AI.",
            icon = Icons.Filled.AutoAwesome,
            tag = "batch_ocr",
            accentColor = VividPurple,
            badgeText = "GEMINI AI"
        ),
        ToolItem(
            title = "PDF Merge & Combine",
            description = "Combine multiple PDF files or receipts into a single organized document.",
            icon = Icons.Filled.Merge,
            tag = "pdf_merge",
            accentColor = VividEmerald,
            badgeText = "POPULAR"
        ),
        ToolItem(
            title = "Image to Searchable Text",
            description = "Convert photos from camera gallery directly to editable TXT or PDF.",
            icon = Icons.Filled.Image,
            tag = "image_to_text",
            accentColor = VividTeal
        ),
        ToolItem(
            title = "PDF Compress & Shrink",
            description = "Reduce document file size up to 70% while maintaining crisp readability.",
            icon = Icons.Filled.Compress,
            tag = "pdf_compress",
            accentColor = VividRose,
            badgeText = "SAVE SPACE"
        ),
        ToolItem(
            title = "Custom E-Watermark",
            description = "Apply brand logos, copyright stamps, or CONFIDENTIAL marks across pages.",
            icon = Icons.Filled.BrandingWatermark,
            tag = "custom_watermark",
            accentColor = VividAmber
        ),
        ToolItem(
            title = "256-Bit AES Encryption",
            description = "Protect confidential documents with password lock and PIN code.",
            icon = Icons.Filled.Lock,
            tag = "aes_lock",
            accentColor = VividCrimson,
            badgeText = "SECURITY"
        )
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("PDF & Scanner Tools", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        bottomBar = {
            ScanBottomBar(
                currentRoute = "tools",
                onNavigate = onNavigate,
                onOpenScan = onOpenScan
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        LazyColumn(
            contentPadding = PaddingValues(
                top = innerPadding.calculateTopPadding() + 12.dp,
                bottom = innerPadding.calculateBottomPadding() + 24.dp,
                start = 20.dp,
                end = 20.dp
            ),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(toolItems) { tool ->
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .clickable {
                            viewModel.showToast("Launched ${tool.title}")
                        }
                        .border(1.dp, tool.accentColor.copy(alpha = 0.25f), RoundedCornerShape(20.dp))
                        .testTag("tool_card_${tool.tag}")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = tool.accentColor.copy(alpha = 0.14f),
                            modifier = Modifier.size(52.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(tool.icon, contentDescription = tool.title, tint = tool.accentColor, modifier = Modifier.size(28.dp))
                            }
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = tool.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.weight(1f, fill = false)
                                )

                                if (tool.badgeText != null) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = tool.accentColor.copy(alpha = 0.15f)
                                    ) {
                                        Text(
                                            text = tool.badgeText,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = tool.accentColor,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = tool.description,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

