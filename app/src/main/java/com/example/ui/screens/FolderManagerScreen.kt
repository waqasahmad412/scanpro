package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Merge
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Share
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.components.DocumentCard
import com.example.ui.components.EmptyStateIllustration
import com.example.ui.components.ScanBottomBar
import com.example.ui.theme.IndigoPrimary
import com.example.ui.theme.WarmOrangeSecondary
import com.example.ui.viewmodel.ScanViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolderManagerScreen(
    viewModel: ScanViewModel,
    onNavigate: (String) -> Unit,
    onOpenDocument: (String) -> Unit,
    onOpenScan: () -> Unit,
    modifier: Modifier = Modifier
) {
    val documents by viewModel.filteredDocuments.collectAsState()
    val folders by viewModel.allFolders.collectAsState()
    val isGridMode by viewModel.isFolderGridMode.collectAsState()
    val isMultiSelect by viewModel.isMultiSelectMode.collectAsState()
    val selectedDocIds by viewModel.selectedDocIds.collectAsState()
    val selectedFolder by viewModel.selectedFolderName.collectAsState()
    val sharingDoc by viewModel.sharingDocument.collectAsState()
    val sharingUris by viewModel.sharingPageUris.collectAsState()

    var showNewFolderDialog by remember { mutableStateOf(false) }
    var newFolderName by remember { mutableStateOf("") }

    if (sharingDoc != null) {
        com.example.ui.components.ShareModal(
            docTitle = sharingDoc?.title ?: "Scanned Document",
            imageUris = sharingUris,
            ocrText = sharingDoc?.ocrText ?: "",
            onDismiss = { viewModel.clearShareState() }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(selectedFolder ?: "File Manager", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { viewModel.isFolderGridMode.value = !isGridMode }) {
                        Icon(
                            if (isGridMode) Icons.Filled.List else Icons.Filled.GridView,
                            contentDescription = "Toggle View Mode",
                            tint = IndigoPrimary
                        )
                    }
                    IconButton(onClick = { viewModel.isMultiSelectMode.value = !isMultiSelect }) {
                        Icon(
                            Icons.Filled.SelectAll,
                            contentDescription = "Select Mode",
                            tint = if (isMultiSelect) WarmOrangeSecondary else IndigoPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        bottomBar = {
            if (isMultiSelect) {
                // Batch Actions Bar
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 12.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp)
                    ) {
                        Button(
                            onClick = { viewModel.mergeSelectedDocuments() },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
                            enabled = selectedDocIds.size >= 2
                        ) {
                            Icon(Icons.Filled.Merge, contentDescription = null)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Merge (${selectedDocIds.size})")
                        }

                        Button(
                            onClick = { viewModel.deleteSelectedDocuments() },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            enabled = selectedDocIds.isNotEmpty()
                        ) {
                            Icon(Icons.Outlined.Delete, contentDescription = null)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Delete")
                        }
                    }
                }
            } else {
                ScanBottomBar(
                    currentRoute = "folders",
                    onNavigate = onNavigate,
                    onOpenScan = onOpenScan
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Folder Filter Chips Carousel
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                item {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                    ) {
                        FilterChip(
                            selected = selectedFolder == null,
                            onClick = { viewModel.selectedFolderName.value = null },
                            label = { Text("All Folders") },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = IndigoPrimary, selectedLabelColor = Color.White)
                        )

                        folders.forEach { f ->
                            FilterChip(
                                selected = selectedFolder == f.name,
                                onClick = { viewModel.selectedFolderName.value = f.name },
                                label = { Text("${f.name} (${f.docCount})") },
                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = IndigoPrimary, selectedLabelColor = Color.White)
                            )
                        }
                    }
                }

                if (documents.isEmpty()) {
                    item {
                        EmptyStateIllustration(
                            title = "Folder is Empty",
                            subtitle = "No scanned documents in this category yet."
                        )
                    }
                } else if (isGridMode) {
                    items(documents.chunked(2)) { pair ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                        ) {
                            pair.forEach { doc ->
                                DocumentCard(
                                    document = doc,
                                    onClick = { onOpenDocument(doc.id) },
                                    onFavoriteToggle = { viewModel.toggleFavorite(doc) },
                                    onDelete = { viewModel.deleteDocument(doc) },
                                    onShare = { viewModel.prepareShareDocument(doc) },
                                    isSelected = selectedDocIds.contains(doc.id),
                                    isSelectionMode = isMultiSelect,
                                    onSelectToggle = { viewModel.toggleSelectDoc(doc.id) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            if (pair.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                } else {
                    items(documents, key = { it.id }) { doc ->
                        DocumentCard(
                            document = doc,
                            onClick = { onOpenDocument(doc.id) },
                            onFavoriteToggle = { viewModel.toggleFavorite(doc) },
                            onDelete = { viewModel.deleteDocument(doc) },
                            onShare = { viewModel.prepareShareDocument(doc) },
                            isSelected = selectedDocIds.contains(doc.id),
                            isSelectionMode = isMultiSelect,
                            onSelectToggle = { viewModel.toggleSelectDoc(doc.id) },
                            modifier = Modifier.padding(vertical = 6.dp)
                        )
                    }
                }
            }
        }
    }
}
