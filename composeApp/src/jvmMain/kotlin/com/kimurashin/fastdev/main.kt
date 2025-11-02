package com.kimurashin.fastdev

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