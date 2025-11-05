package com.kimurashin.fastdev.presentation.screens

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "FastDev",
    ) {
        App()
    }
}