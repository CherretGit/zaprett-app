package com.cherret.zaprett.data

data class ListUiState(
    val items: List<ListUiItem> = emptyList(),
    val isRefreshing: Boolean = false,
    val error: String? = null,
)

data class ListUiItem(
    val data: StorageData,
    val isChecked: Boolean,
    val isUsing: Boolean
)