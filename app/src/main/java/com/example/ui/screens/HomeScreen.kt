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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.components.DocumentCard
import com.example.ui.components.EmptyStateIllustration
import com.example.ui.components.FolderCard
import com.example.ui.components.ScanBottomBar
import com.example.ui.theme.IndigoPrimary
import com.example.ui.theme.WarmOrangeSecondary
import com.example.ui.viewmodel.ScanViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: ScanViewModel,
    onNavigate: (String) -> Unit,
    onOpenDocument: (String) -> Unit,
    onOpenScan: () -> Unit,
    onOpenFolder: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val documents by viewModel.filteredDocuments.collectAsState()
    val folders by viewModel.allFolders.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val sharingDoc by viewModel.sharingDocument.collectAsState()
    val sharingUris by viewModel.sharingPageUris.collectAsState()
    var showOnlyFavorites by remember { mutableStateOf(false) }

    val currentUser by viewModel.currentUser.collectAsState()
    val recentDocs = remember(documents, showOnlyFavorites) {
        if (showOnlyFavorites) documents.filter { it.isFavorite } else documents
    }

    if (sharingDoc != null) {
        com.example.ui.components.ShareModal(
            docTitle = sharingDoc?.title ?: "Scanned Document",
            imageUris = sharingUris,
            ocrText = sharingDoc?.ocrText ?: "",
            onDismiss = { viewModel.clearShareState() }
        )
    }

    Scaffold(
        bottomBar = {
            ScanBottomBar(
                currentRoute = "home",
                onNavigate = onNavigate,
                onOpenScan = onOpenScan
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        LazyColumn(
            contentPadding = PaddingValues(
                top = innerPadding.calculateTopPadding() + 16.dp,
                bottom = innerPadding.calculateBottomPadding() + 24.dp
            ),
            modifier = Modifier.fillMaxSize()
        ) {
            // Header Title & Cloud Status
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                ) {
                    Column {
                        Text(
                            text = "Hi, ${currentUser.name}",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = IndigoPrimary
                        )
                        Text(
                            text = if (currentUser.isLoggedIn) currentUser.email else "Guest Workspace • Tap to Sign In",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val context = androidx.compose.ui.platform.LocalContext.current
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = Color(0xFF1A73E8).copy(alpha = 0.12f),
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .clickable {
                                    val appUrl = "https://ais-pre-v7d6yy75crgfehd5sxig25-159852313730.asia-east1.run.app"
                                    val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(android.content.Intent.EXTRA_SUBJECT, "Download ScanPro AI App")
                                        putExtra(
                                            android.content.Intent.EXTRA_TEXT,
                                            "Install ScanPro AI on your mobile phone: $appUrl"
                                        )
                                    }
                                    context.startActivity(android.content.Intent.createChooser(shareIntent, "Share App Link"))
                                }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Share,
                                    contentDescription = "Share App",
                                    tint = Color(0xFF1A73E8),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Share App",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF1A73E8),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = IndigoPrimary.copy(alpha = 0.12f),
                            modifier = Modifier.clickable {
                                if (!currentUser.isLoggedIn) {
                                    onNavigate("auth")
                                } else {
                                    onNavigate("settings")
                                }
                            }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    imageVector = if (currentUser.isLoggedIn) Icons.Filled.CloudSync else Icons.Filled.Person,
                                    contentDescription = null,
                                    tint = IndigoPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (currentUser.isLoggedIn) "Pro User" else "Sign In",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = IndigoPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            // Top Search Bar
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.searchQuery.value = it },
                    placeholder = { Text("Search scans, receipts, or OCR text...") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Search", tint = IndigoPrimary) },
                    trailingIcon = if (searchQuery.isNotEmpty()) {
                        {
                            IconButton(onClick = { viewModel.searchQuery.value = "" }) {
                                Icon(Icons.Filled.Clear, contentDescription = "Clear")
                            }
                        }
                    } else null,
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedBorderColor = IndigoPrimary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .testTag("home_search_bar")
                )

                Spacer(modifier = Modifier.height(18.dp))
            }

            // Quick Action Cards Carousel
            item {
                Text(
                    text = "Quick Actions",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        Card(
                            onClick = {
                                viewModel.scanMode.value = com.example.data.model.ScanMode.SINGLE
                                onOpenScan()
                            },
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = WarmOrangeSecondary.copy(alpha = 0.12f)),
                            modifier = Modifier
                                .width(130.dp)
                                .border(1.dp, WarmOrangeSecondary.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.Start
                            ) {
                                Surface(shape = CircleShape, color = WarmOrangeSecondary, modifier = Modifier.size(36.dp)) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Filled.Camera, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                                    }
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                Text("Doc Scan", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = WarmOrangeSecondary)
                                Text("Auto edge detect", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }

                    item {
                        Card(
                            onClick = {
                                viewModel.scanMode.value = com.example.data.model.ScanMode.ID_CARD
                                onOpenScan()
                            },
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = IndigoPrimary.copy(alpha = 0.12f)),
                            modifier = Modifier
                                .width(130.dp)
                                .border(1.dp, IndigoPrimary.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.Start
                            ) {
                                Surface(shape = CircleShape, color = IndigoPrimary, modifier = Modifier.size(36.dp)) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Filled.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                                    }
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                Text("ID & Passport", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = IndigoPrimary)
                                Text("2-Sided Align", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }

                    item {
                        Card(
                            onClick = {
                                viewModel.scanMode.value = com.example.data.model.ScanMode.BATCH
                                onOpenScan()
                            },
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = com.example.ui.theme.VividEmerald.copy(alpha = 0.12f)),
                            modifier = Modifier
                                .width(130.dp)
                                .border(1.dp, com.example.ui.theme.VividEmerald.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.Start
                            ) {
                                Surface(shape = CircleShape, color = com.example.ui.theme.VividEmerald, modifier = Modifier.size(36.dp)) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Filled.FilterList, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                                    }
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                Text("Batch Mode", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = com.example.ui.theme.VividEmerald)
                                Text("Multi-page scan", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }

                    item {
                        Card(
                            onClick = { onNavigate("tools") },
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = com.example.ui.theme.VividPurple.copy(alpha = 0.12f)),
                            modifier = Modifier
                                .width(130.dp)
                                .border(1.dp, com.example.ui.theme.VividPurple.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.Start
                            ) {
                                Surface(shape = CircleShape, color = com.example.ui.theme.VividPurple, modifier = Modifier.size(36.dp)) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                                    }
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                Text("Gemini OCR", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = com.example.ui.theme.VividPurple)
                                Text("AI Text Extract", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
            }

            // Quick Filters Bar
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                ) {
                    FilterChip(
                        selected = !showOnlyFavorites,
                        onClick = { showOnlyFavorites = false },
                        label = { Text("All Scans") },
                        shape = RoundedCornerShape(10.dp),
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = IndigoPrimary, selectedLabelColor = Color.White)
                    )

                    FilterChip(
                        selected = showOnlyFavorites,
                        onClick = { showOnlyFavorites = true },
                        label = { Text("Favorites") },
                        leadingIcon = { Icon(Icons.Filled.Star, contentDescription = null, tint = WarmOrangeSecondary, modifier = Modifier.size(16.dp)) },
                        shape = RoundedCornerShape(10.dp),
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = IndigoPrimary, selectedLabelColor = Color.White)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
            }

            // Recent Scans Section (Horizontal Card Carousel)
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                ) {
                    Text(
                        text = "Recent Scans",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    TextButton(onClick = { onNavigate("folders") }) {
                        Text("View All (${recentDocs.size})", color = IndigoPrimary, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (recentDocs.isEmpty()) {
                    EmptyStateIllustration(
                        title = "No Scans Found",
                        subtitle = if (searchQuery.isNotEmpty()) "No documents match '$searchQuery'" else "Tap New Scan to capture your first document."
                    )
                } else {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(recentDocs, key = { it.id }) { doc ->
                            DocumentCard(
                                document = doc,
                                onClick = { onOpenDocument(doc.id) },
                                onFavoriteToggle = { viewModel.toggleFavorite(doc) },
                                onDelete = { viewModel.deleteDocument(doc) },
                                onShare = { viewModel.prepareShareDocument(doc) },
                                modifier = Modifier.width(220.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))
            }

            // Categories / Folder Grid
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                ) {
                    Text(
                        text = "Folders & Categories",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    TextButton(onClick = { onNavigate("folders") }) {
                        Text("Manage", color = IndigoPrimary, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
            }

            // Folder Rows (2 columns)
            items(folders.chunked(2)) { pair ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 6.dp)
                ) {
                    pair.forEach { folder ->
                        FolderCard(
                            folder = folder,
                            onClick = { onOpenFolder(folder.name) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (pair.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}
