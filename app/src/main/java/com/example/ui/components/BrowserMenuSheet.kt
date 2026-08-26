package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DesktopMac
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FindInPage
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Shortcut
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.tabs.WebTab

/**
 * High-End Browser Feature Menu with full controls:
 * - AdBlocker
 * - Desktop Site
 * - Dark Mode
 * - Bookmarks / History / Downloads
 * - Translate
 * - Find in page
 * - Save as PDF
 * - Add shortcut
 * - Text zoom
 * - Basic Settings
 * - About & Legal (La Clave Argentina & Tienda SSH)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowserMenuSheet(
    activeTab: WebTab?,
    isBookmarked: Boolean,
    isAdBlockerActive: Boolean,
    onToggleBookmark: () -> Unit,
    onToggleAdBlocker: () -> Unit,
    onToggleDarkMode: () -> Unit,
    onToggleDesktopMode: () -> Unit,
    onSetTextZoom: (Int) -> Unit,
    onNewTab: () -> Unit,
    onNewIncognitoTab: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenBookmarks: () -> Unit,
    onOpenDownloads: () -> Unit,
    onFindInPage: () -> Unit,
    onTranslate: () -> Unit,
    onSavePdf: () -> Unit,
    onAddShortcut: () -> Unit,
    onShare: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenAbout: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var textZoomValue by remember(activeTab?.textZoom) {
        mutableFloatStateOf((activeTab?.textZoom ?: 100).toFloat())
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = Modifier.testTag("browser_menu_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Quick Top Action Row (Bookmark, Downloads, History, Share)
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            onToggleBookmark()
                        },
                        modifier = Modifier.testTag("menu_bookmark_button")
                    ) {
                        Icon(
                            imageVector = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                            contentDescription = "Bookmark",
                            tint = if (isBookmarked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }

                    IconButton(
                        onClick = {
                            onDismiss()
                            onOpenDownloads()
                        },
                        modifier = Modifier.testTag("menu_downloads_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = "Downloads"
                        )
                    }

                    IconButton(
                        onClick = {
                            onDismiss()
                            onOpenHistory()
                        },
                        modifier = Modifier.testTag("menu_history_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = "History"
                        )
                    }

                    IconButton(
                        onClick = {
                            onDismiss()
                            onShare()
                        },
                        modifier = Modifier.testTag("menu_share_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share"
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Primary Switches
            MenuItemSwitch(
                icon = Icons.Default.Shield,
                title = "Bloqueador de Anuncios",
                subtitle = "Filtrado O(1) de publicidad y rastreadores",
                isChecked = isAdBlockerActive,
                onCheckedChange = { onToggleAdBlocker() },
                testTag = "ad_blocker_switch"
            )

            MenuItemSwitch(
                icon = Icons.Default.DesktopMac,
                title = "Sitio para Computadora",
                subtitle = "Simular navegador de escritorio",
                isChecked = activeTab?.isDesktopMode == true,
                onCheckedChange = { onToggleDesktopMode() },
                testTag = "desktop_site_switch"
            )

            MenuItemSwitch(
                icon = Icons.Default.DarkMode,
                title = "Modo Oscuro Web",
                subtitle = "Forzar fondo oscuro en páginas web",
                isChecked = activeTab?.isDarkModeEnabled == true,
                onCheckedChange = { onToggleDarkMode() },
                testTag = "dark_mode_switch"
            )

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 10.dp),
                color = DividerDefaults.color.copy(alpha = 0.4f)
            )

            // Text Zoom / Size Slider
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.FormatSize,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Tamaño del Texto",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Text(
                        text = "${textZoomValue.toInt()}%",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Slider(
                    value = textZoomValue,
                    onValueChange = {
                        textZoomValue = it
                        onSetTextZoom(it.toInt())
                    },
                    valueRange = 50f..200f,
                    steps = 14,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("text_zoom_slider")
                )
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 10.dp),
                color = DividerDefaults.color.copy(alpha = 0.4f)
            )

            // Navigation Actions
            MenuItemRow(
                icon = Icons.Default.Add,
                title = "Nueva Pestaña",
                onClick = {
                    onDismiss()
                    onNewTab()
                },
                testTag = "menu_new_tab"
            )

            MenuItemRow(
                icon = Icons.Default.VisibilityOff,
                title = "Nueva Pestaña de Incógnito",
                onClick = {
                    onDismiss()
                    onNewIncognitoTab()
                },
                testTag = "menu_new_incognito"
            )

            MenuItemRow(
                icon = Icons.Default.Bookmark,
                title = "Marcadores Guardados",
                onClick = {
                    onDismiss()
                    onOpenBookmarks()
                },
                testTag = "menu_bookmarks"
            )

            MenuItemRow(
                icon = Icons.Default.FindInPage,
                title = "Buscar en la Página",
                onClick = {
                    onDismiss()
                    onFindInPage()
                },
                testTag = "menu_find_in_page"
            )

            MenuItemRow(
                icon = Icons.Default.Translate,
                title = "Traducir Página",
                onClick = {
                    onDismiss()
                    onTranslate()
                },
                testTag = "menu_translate"
            )

            MenuItemRow(
                icon = Icons.Default.PictureAsPdf,
                title = "Guardar como PDF",
                onClick = {
                    onDismiss()
                    onSavePdf()
                },
                testTag = "menu_save_pdf"
            )

            MenuItemRow(
                icon = Icons.Default.Shortcut,
                title = "Añadir a Pantalla de Inicio",
                onClick = {
                    onDismiss()
                    onAddShortcut()
                },
                testTag = "menu_add_shortcut"
            )

            MenuItemRow(
                icon = Icons.Default.Settings,
                title = "Configuración Básica",
                subtitle = "Buscador, barra de navegación, caché",
                onClick = {
                    onDismiss()
                    onOpenSettings()
                },
                testTag = "menu_settings"
            )

            MenuItemRow(
                icon = Icons.Default.Info,
                title = "Acerca de y Legal",
                subtitle = "Créditos: La Clave Argentina & Tienda SSH",
                onClick = {
                    onDismiss()
                    onOpenAbout()
                },
                testTag = "menu_about_legal"
            )
        }
    }
}

@Composable
fun MenuItemRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 10.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
fun MenuItemSwitch(
    icon: ImageVector,
    title: String,
    subtitle: String,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isChecked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )
            }
        }

        Switch(
            checked = isChecked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.testTag(testTag)
        )
    }
}
