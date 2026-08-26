package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Tab
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.tabs.WebTab

/**
 * Modern Aerodynamic Address Bar with integrated loading indicator,
 * SSL encryption lock, Tab switcher badge, and quick navigation actions.
 */
@Composable
fun BrowserAddressBar(
    tab: WebTab?,
    tabCount: Int,
    isBottomBar: Boolean,
    isAdBlockActive: Boolean,
    onNavigate: (String) -> Unit,
    onReload: () -> Unit,
    onStop: () -> Unit,
    onGoBack: () -> Unit,
    onGoForward: () -> Unit,
    onOpenTabs: () -> Unit,
    onOpenMenu: () -> Unit,
    onOpenQrScanner: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isEditing by remember { mutableStateOf(false) }
    var textInput by remember(tab?.url) { mutableStateOf(tab?.url ?: "") }
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    val isSecure = tab?.url?.startsWith("https://", ignoreCase = true) == true
    val isIncognito = tab?.isIncognito == true

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .then(if (isBottomBar) Modifier.navigationBarsPadding() else Modifier.statusBarsPadding()),
        tonalElevation = 6.dp,
        color = if (isIncognito) Color(0xFF161B22) else MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Ultra-fine loading progress bar
            if (tab?.isLoading == true && tab.progress in 1..99) {
                LinearProgressIndicator(
                    progress = { tab.progress / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.5.dp)
                        .testTag("web_progress_bar"),
                    color = if (isIncognito) Color(0xFF00E5FF) else MaterialTheme.colorScheme.primary,
                    trackColor = Color.Transparent
                )
            } else {
                Spacer(modifier = Modifier.height(2.5.dp))
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Back button
                IconButton(
                    onClick = onGoBack,
                    enabled = tab?.canGoBack == true,
                    modifier = Modifier.testTag("nav_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Go Back",
                        tint = if (tab?.canGoBack == true) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                        }
                    )
                }

                // Forward button
                IconButton(
                    onClick = onGoForward,
                    enabled = tab?.canGoForward == true,
                    modifier = Modifier.testTag("nav_forward_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Go Forward",
                        tint = if (tab?.canGoForward == true) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                        }
                    )
                }

                // Interactive Address/Search Box
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(
                            if (isIncognito) Color(0xFF21262D) else MaterialTheme.colorScheme.surfaceVariant
                        )
                        .clickable(enabled = !isEditing) {
                            textInput = tab?.url ?: ""
                            isEditing = true
                        }
                        .padding(horizontal = 12.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // SSL or Incognito or Security Indicator
                        if (isIncognito) {
                            Icon(
                                imageVector = Icons.Default.VisibilityOff,
                                contentDescription = "Incognito",
                                tint = Color(0xFF00E5FF),
                                modifier = Modifier.size(18.dp)
                            )
                        } else if (isSecure) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Secure HTTPS",
                                tint = Color(0xFF2E7D32),
                                modifier = Modifier.size(18.dp)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Insecure HTTP",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        if (isEditing) {
                            BasicTextField(
                                value = textInput,
                                onValueChange = { textInput = it },
                                singleLine = true,
                                textStyle = TextStyle(
                                    color = if (isIncognito) Color.White else MaterialTheme.colorScheme.onSurface,
                                    fontSize = 15.sp
                                ),
                                cursorBrush = SolidColor(
                                    if (isIncognito) Color(0xFF00E5FF) else MaterialTheme.colorScheme.primary
                                ),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                                keyboardActions = KeyboardActions(
                                    onGo = {
                                        isEditing = false
                                        focusManager.clearFocus()
                                        if (textInput.isNotBlank()) {
                                            onNavigate(textInput)
                                        }
                                    }
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .focusRequester(focusRequester)
                                    .testTag("url_text_input")
                            )

                            LaunchedEffect(Unit) {
                                focusRequester.requestFocus()
                            }

                            if (textInput.isNotEmpty()) {
                                IconButton(
                                    onClick = { textInput = "" },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Clear text",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        } else {
                            // Display simplified URL or Title
                            val displayDomain = remember(tab?.url) {
                                try {
                                    val uri = android.net.Uri.parse(tab?.url ?: "")
                                    uri.host ?: tab?.url ?: "Search or enter URL"
                                } catch (_: Exception) {
                                    tab?.url ?: "Search or enter URL"
                                }
                            }

                            Text(
                                text = displayDomain,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = if (isIncognito) Color.White else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )

                            // Quick QR scan button in URL bar
                            IconButton(
                                onClick = onOpenQrScanner,
                                modifier = Modifier
                                    .size(32.dp)
                                    .testTag("qr_scan_action")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.QrCodeScanner,
                                    contentDescription = "Scan QR",
                                    tint = if (isIncognito) Color(0xFF00E5FF) else MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                // Reload or Stop Button
                IconButton(
                    onClick = {
                        if (tab?.isLoading == true) onStop() else onReload()
                    },
                    modifier = Modifier.testTag("reload_stop_button")
                ) {
                    Icon(
                        imageVector = if (tab?.isLoading == true) Icons.Default.Close else Icons.Default.Refresh,
                        contentDescription = if (tab?.isLoading == true) "Stop" else "Reload"
                    )
                }

                // Tab Switcher Button with Badge
                IconButton(
                    onClick = onOpenTabs,
                    modifier = Modifier.testTag("tab_switcher_button")
                ) {
                    BadgedBox(
                        badge = {
                            Badge(
                                containerColor = if (isIncognito) Color(0xFF00E5FF) else MaterialTheme.colorScheme.primary,
                                contentColor = if (isIncognito) Color.Black else MaterialTheme.colorScheme.onPrimary
                            ) {
                                Text(
                                    text = tabCount.toString(),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tab,
                            contentDescription = "Tabs"
                        )
                    }
                }

                // Menu Overflow
                IconButton(
                    onClick = onOpenMenu,
                    modifier = Modifier.testTag("browser_menu_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Menu"
                    )
                }
            }
        }
    }
}
