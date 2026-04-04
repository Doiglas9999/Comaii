package com.comaii.app.ui.util

fun Double.formatPrice(): String {
    val intPart = this.toLong()
    val decPart = ((this - intPart) * 100).toLong().let { if (it < 0) -it else it }
    return "$intPart,${decPart.toString().padStart(2, '0')}"
}
