package com.example.ui.components

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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Tab
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.tabs.WebTab

/**
 * Advanced Visual Tab Switcher with Incognito filtering and dynamic grid layout.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TabSwitcherSheet(
    tabs: List<WebTab>,
    activeTabId: String,
    onSelectTab: (String) -> Unit,
    onCloseTab: (String) -> Unit,
    onNewTab: (Boolean) -> Unit,
    onCloseAllTabs: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showIncognito by remember {
        mutableStateOf(tabs.find { it.id == activeTabId }?.isIncognito ?: false)
    }

    val filteredTabs = remember(tabs, showIncognito) {
        tabs.filter { it.isIncognito == showIncognito }
    }

    val normalCount = remember(tabs) { tabs.count { !it.isIncognito } }
    val incognitoCount = remember(tabs) { tabs.count { it.isIncognito } }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = if (showIncognito) Color(0xFF0D1117) else MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .fillMaxHeight(0.92f)
            .testTag("tab_switcher_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            // Header & Filter Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = !showIncognito,
                        onClick = { showIncognito = false },
                        label = { Text("Standard ($normalCount)") },
                        leadingIcon = {
                            Icon(Icons.Default.Tab, contentDescription = null, modifier = Modifier.size(16.dp))
                        },
                        modifier = Modifier.testTag("standard_tabs_chip")
                    )

                    FilterChip(
                        selected = showIncognito,
                        onClick = { showIncognito = true },
                        label = { Text("Incognito ($incognitoCount)") },
                        leadingIcon = {
                            Icon(Icons.Default.VisibilityOff, contentDescription = null, modifier = Modifier.size(16.dp))
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF1F2937),
                            selectedLabelColor = Color(0xFF00E5FF),
                            selectedLeadingIconColor = Color(0xFF00E5FF)
                        ),
                        modifier = Modifier.testTag("incognito_tabs_chip")
                    )
                }

                if (filteredTabs.isNotEmpty()) {
                    IconButton(
                        onClick = { onCloseAllTabs(showIncognito) },
                        modifier = Modifier.testTag("close_all_tabs_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteSweep,
                            contentDescription = "Close all tabs",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Tabs Grid
            if (filteredTabs.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = if (showIncognito) Icons.Default.VisibilityOff else Icons.Default.Tab,
                            contentDescription = null,
                            tint = if (showIncognito) Color(0xFF00E5FF) else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (showIncognito) "No Incognito Tabs Open" else "No Standard Tabs Open",
                            style = MaterialTheme.typography.titleMedium,
                            color = if (showIncognito) Color.White else MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { onNewTab(showIncognito) },
                            colors = if (showIncognito) {
                                ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF00E5FF),
                                    contentColor = Color.Black
                                )
                            } else {
                                ButtonDefaults.buttonColors()
                            }
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (showIncognito) "New Incognito Tab" else "New Tab")
                        }
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredTabs, key = { it.id }) { tab ->
                        val isSelected = tab.id == activeTabId
                        TabCard(
                            tab = tab,
                            isSelected = isSelected,
                            onSelect = {
                                onSelectTab(tab.id)
                                onDismiss()
                            },
                            onClose = { onCloseTab(tab.id) }
                        )
                    }
                }
            }

            // Bottom Actions
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = { onNewTab(false) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("new_standard_tab_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("New Tab")
                }

                OutlinedButton(
                    onClick = { onNewTab(true) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("new_incognito_tab_button"),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFF00E5FF)
                    )
                ) {
                    Icon(Icons.Default.VisibilityOff, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Incognito")
                }
            }
        }
    }
}

@Composable
fun TabCard(
    tab: WebTab,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onClose: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(130.dp)
            .clip(RoundedCornerShape(16.dp))
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) {
                    if (tab.isIncognito) Color(0xFF00E5FF) else MaterialTheme.colorScheme.primary
                } else {
                    if (tab.isIncognito) Color(0xFF30363D) else MaterialTheme.colorScheme.outlineVariant
                },
                shape = RoundedCornerShape(16.dp)
            )
            .clickable { onSelect() }
            .testTag("tab_card_${tab.id}"),
        colors = CardDefaults.cardColors(
            containerColor = if (tab.isIncognito) Color(0xFF161B22) else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header: Favicon + Title + Close Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (tab.favicon != null) {
                    Image(
                        bitmap = tab.favicon.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier
                            .size(18.dp)
                            .clip(CircleShape)
                    )
                } else {
                    Icon(
                        imageVector = if (tab.isIncognito) Icons.Default.VisibilityOff else Icons.Default.Public,
                        contentDescription = null,
                        tint = if (tab.isIncognito) Color(0xFF00E5FF) else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                Text(
                    text = tab.title,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = if (tab.isIncognito) Color.White else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )

                IconButton(
                    onClick = onClose,
                    modifier = Modifier
                        .size(24.dp)
                        .testTag("close_tab_${tab.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close Tab",
                        tint = if (tab.isIncognito) Color.LightGray else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Body: URL Preview
            Text(
                text = tab.url,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = if (tab.isIncognito) Color(0xFF8B949E) else MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Bottom Badges
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (tab.isDesktopMode) {
                    Text(
                        text = "DESKTOP",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }

                if (isSelected) {
                    Surface(
                        shape = CircleShape,
                        color = if (tab.isIncognito) Color(0xFF00E5FF) else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(8.dp)
                    ) {}
                }
            }
        }
    }
}
