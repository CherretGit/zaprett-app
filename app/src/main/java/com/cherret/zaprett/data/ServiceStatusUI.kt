package com.cherret.zaprett.data

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.ui.graphics.vector.ImageVector
import com.cherret.zaprett.R

data class ServiceStatusUI(
    val textRes: Int = R.string.status_not_availible,
    val icon: ImageVector = Icons.AutoMirrored.Filled.Help
)