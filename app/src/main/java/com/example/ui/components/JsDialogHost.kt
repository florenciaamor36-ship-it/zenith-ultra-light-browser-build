package com.example.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.ui.viewmodel.GeolocationState
import com.example.ui.viewmodel.JsAlertState
import com.example.ui.viewmodel.JsConfirmState
import com.example.ui.viewmodel.JsPromptState
import com.example.ui.viewmodel.SslErrorState

/**
 * Material 3 Dialog Host handling web-origin requests:
 * - JavaScript alert / confirm / prompt dialogs
 * - HTML5 Geolocation permission queries
 * - SSL Certificate warning dialogs
 */
@Composable
fun JsDialogHost(
    alertState: JsAlertState?,
    confirmState: JsConfirmState?,
    promptState: JsPromptState?,
    geolocationState: GeolocationState?,
    sslErrorState: SslErrorState?,
    onDismissAlert: () -> Unit,
    onDismissConfirm: (Boolean) -> Unit,
    onDismissPrompt: (String?) -> Unit,
    onDismissGeo: (Boolean) -> Unit,
    onDismissSsl: (Boolean) -> Unit
) {
    // 1. JS Alert
    alertState?.let { alert ->
        AlertDialog(
            onDismissRequest = {
                alert.result.cancel()
                onDismissAlert()
            },
            title = { Text("Webpage Notice") },
            text = { Text(alert.message) },
            confirmButton = {
                TextButton(
                    onClick = {
                        alert.result.confirm()
                        onDismissAlert()
                    },
                    modifier = Modifier.testTag("js_alert_ok")
                ) {
                    Text("OK")
                }
            }
        )
    }

    // 2. JS Confirm
    confirmState?.let { confirm ->
        AlertDialog(
            onDismissRequest = {
                confirm.result.cancel()
                onDismissConfirm(false)
            },
            title = { Text("Webpage Confirmation") },
            text = { Text(confirm.message) },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirm.result.confirm()
                        onDismissConfirm(true)
                    },
                    modifier = Modifier.testTag("js_confirm_ok")
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        confirm.result.cancel()
                        onDismissConfirm(false)
                    },
                    modifier = Modifier.testTag("js_confirm_cancel")
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // 3. JS Prompt
    promptState?.let { prompt ->
        var promptInput by remember { mutableStateOf(prompt.defaultValue) }
        AlertDialog(
            onDismissRequest = {
                prompt.result.cancel()
                onDismissPrompt(null)
            },
            title = { Text("Webpage Prompt") },
            text = {
                Column {
                    Text(prompt.message)
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = promptInput,
                        onValueChange = { promptInput = it },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        prompt.result.confirm(promptInput)
                        onDismissPrompt(promptInput)
                    },
                    modifier = Modifier.testTag("js_prompt_ok")
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        prompt.result.cancel()
                        onDismissPrompt(null)
                    },
                    modifier = Modifier.testTag("js_prompt_cancel")
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // 4. Geolocation Dialog
    geolocationState?.let { geo ->
        AlertDialog(
            onDismissRequest = {
                geo.callback.invoke(geo.origin, false, false)
                onDismissGeo(false)
            },
            icon = {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            title = { Text("Location Request") },
            text = { Text("${geo.origin} wants to access your device's location.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        geo.callback.invoke(geo.origin, true, false)
                        onDismissGeo(true)
                    },
                    modifier = Modifier.testTag("geo_allow")
                ) {
                    Text("Allow")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        geo.callback.invoke(geo.origin, false, false)
                        onDismissGeo(false)
                    },
                    modifier = Modifier.testTag("geo_block")
                ) {
                    Text("Block")
                }
            }
        )
    }

    // 5. SSL Error Dialog
    sslErrorState?.let { ssl ->
        AlertDialog(
            onDismissRequest = {
                ssl.handler.cancel()
                onDismissSsl(false)
            },
            icon = {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = { Text("Security Warning") },
            text = { Text("The security certificate of this website is invalid or untrusted. Proceeding may expose your private information.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        ssl.handler.proceed()
                        onDismissSsl(true)
                    }
                ) {
                    Text("Proceed anyway", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        ssl.handler.cancel()
                        onDismissSsl(false)
                    }
                ) {
                    Text("Go Back")
                }
            }
        )
    }
}
