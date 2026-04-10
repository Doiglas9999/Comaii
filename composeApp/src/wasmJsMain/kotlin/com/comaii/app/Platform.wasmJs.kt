package com.comaii.app

import kotlinx.browser.window

actual fun getStoreBaseUrl(): String = window.location.host
