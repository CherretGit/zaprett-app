package com.cherret.zaprett.data

sealed class Setting {
    data class Toggle(val title: String, val checked: Boolean, val onToggle: (Boolean) -> Unit) : Setting()
    data class Action(val title: String, val onClick: () -> Unit) : Setting()
    data class Section(val title: String) : Setting()
}