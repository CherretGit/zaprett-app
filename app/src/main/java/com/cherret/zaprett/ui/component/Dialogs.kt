package com.cherret.zaprett.ui.component

import android.content.Context
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.cherret.zaprett.R
import com.cherret.zaprett.data.StorageData

@Composable
fun InfoDialog(title: String, message: String, onDismiss: () -> Unit) {
    AlertDialog(
        title = { Text(text = title) },
        text = { Text(text = message) },
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.btn_continue))
            }
        }
    )
}

@Composable
fun TextDialog(title: String, message: String, initialText: String, onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var inputText by remember { mutableStateOf(initialText) }
    AlertDialog(
        title = { Text(text = title) },
        text = {
            TextField(
                value = inputText,
                onValueChange = { inputText = it },
                placeholder = { Text(message) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )},
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                if (inputText.isNotEmpty()) {
                    onConfirm(inputText)
                    onDismiss()
                }
                else {
                    onDismiss()
                }
            }
            ) {
                Text(stringResource(R.string.btn_continue))
            }
        },
        dismissButton = {
            TextButton(
                onClick = { onDismiss() }
            ) {
                Text(stringResource(R.string.btn_dismiss))
            }
        }
    )
}
@Composable
fun GenerateManifestDialog(path: String, onConfirm: (StorageData) -> Unit, onDismiss: () -> Unit) {
    var id by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var version by remember { mutableStateOf("") }
    var author by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    AlertDialog(
        title = { Text(text = stringResource(R.string.title_generate_manifest)) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextField(
                    value = id,
                    onValueChange = { id = it },
                    placeholder = { Text(stringResource(R.string.hint_manifest_id)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                TextField(
                    value = name,
                    onValueChange = { name = it },
                    placeholder = { Text(stringResource(R.string.hint_manifest_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                TextField(
                    value = version,
                    onValueChange = { version = it },
                    placeholder = { Text(stringResource(R.string.hint_manifest_version)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                TextField(
                    value = author,
                    onValueChange = { author = it },
                    placeholder = { Text(stringResource(R.string.hint_manifest_author)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                TextField(
                    value = description,
                    onValueChange = { description = it },
                    placeholder = { Text(stringResource(R.string.hint_manifest_description)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = { onConfirm(
                    StorageData(
                        schema = 1,
                        id = id,
                        name = name,
                        version = version,
                        author = author,
                        description = description,
                        dependencies = emptyList(),
                        file = path
                    )
                ) }
            ) {
                Text(stringResource(R.string.btn_continue))
            }
        },
        dismissButton = {
            TextButton(onClick = {
                onDismiss()
            }) {
                Text(stringResource(R.string.btn_dismiss))
            }
        },
    )
}