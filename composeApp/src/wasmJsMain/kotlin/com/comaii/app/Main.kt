package com.comaii.app

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.CanvasBasedWindow
import kotlinx.browser.window

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    // Extrai o companyId da URL: comaii.com/{companyId}
    val path = window.location.pathname.removePrefix("/")
    val companyId = path.ifEmpty { "default" }

    CanvasBasedWindow(canvasElementId = "ComposeTarget") {
        App(companyId = companyId)
    }
}
