package com.cherret.zaprett.ui.component

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cherret.zaprett.R

@Composable
fun SettingsItem(title: String, checked: Boolean, onToggle: (Boolean) -> Unit, onCheckedChange: (Boolean) -> Unit) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        onClick = { onToggle(!checked) },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = title,
                modifier = Modifier.weight(1f)
            )
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange
            )
        }
    }
}

@Composable
fun SettingsActionItem(title: String, onClick: () -> Unit) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        onClick = { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text(
                text = title,
                modifier = Modifier.weight(1f)
            )
            Icon(imageVector = Icons.AutoMirrored.Default.ArrowForward, contentDescription = null)
        }
    }
}

@Composable
fun SettingsSection(title: String) {
    Text(
        text = title,
        fontSize = 20.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp)
    )
}

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