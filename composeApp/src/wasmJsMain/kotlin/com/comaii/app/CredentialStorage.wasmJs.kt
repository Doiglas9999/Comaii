package com.comaii.app

import kotlinx.browser.localStorage

actual object CredentialStorage {
    private const val KEY_EMAIL = "comaii_email"
    private const val KEY_PASSWORD = "comaii_password"

    actual fun save(email: String, password: String) {
        localStorage.setItem(KEY_EMAIL, email)
        localStorage.setItem(KEY_PASSWORD, password)
    }

    actual fun load(): Pair<String, String>? {
        val email = localStorage.getItem(KEY_EMAIL) ?: return null
        val password = localStorage.getItem(KEY_PASSWORD) ?: return null
        if (email.isBlank() || password.isBlank()) return null
        return email to password
    }

    actual fun clear() {
        localStorage.removeItem(KEY_EMAIL)
        localStorage.removeItem(KEY_PASSWORD)
    }
}
