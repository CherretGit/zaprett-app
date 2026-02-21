package com.cherret.zaprett.data

sealed class Setting {
    data class Toggle(val title: String, val checked: Boolean, val onToggle: (Boolean) -> Unit) : Setting()
    data class Action(val title: String, val onClick: () -> Unit) : Setting()
    data class Dropdown(val title: String, val selected: String, val items: List<DropdownItem>): Setting()
    data class Section(val title: String) : Setting()
}

data class DropdownItem(
    val title: String,
    val onClick: () -> Unit
)