package com.comaii.app

import android.content.Context
import android.content.SharedPreferences

actual object CredentialStorage {
    private const val PREFS_NAME = "comaii_prefs"
    private const val KEY_EMAIL = "email"
    private const val KEY_PASSWORD = "password"
    private const val KEY_CUST_EMAIL = "cust_email"
    private const val KEY_CUST_PASSWORD = "cust_password"

    private var prefs: SharedPreferences? = null

    /** Deve ser chamado em MainActivity.onCreate antes de usar */
    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    actual fun save(email: String, password: String) {
        prefs?.edit()
            ?.putString(KEY_EMAIL, email)
            ?.putString(KEY_PASSWORD, password)
            ?.apply()
    }

    actual fun load(): Pair<String, String>? {
        val p = prefs ?: return null
        val email = p.getString(KEY_EMAIL, null) ?: return null
        val password = p.getString(KEY_PASSWORD, null) ?: return null
        if (email.isBlank() || password.isBlank()) return null
        return email to password
    }

    actual fun clear() {
        prefs?.edit()?.remove(KEY_EMAIL)?.remove(KEY_PASSWORD)?.apply()
    }

    actual fun saveCustomer(email: String, password: String) {
        prefs?.edit()
            ?.putString(KEY_CUST_EMAIL, email)
            ?.putString(KEY_CUST_PASSWORD, password)
            ?.apply()
    }

    actual fun loadCustomer(): Pair<String, String>? {
        val p = prefs ?: return null
        val email = p.getString(KEY_CUST_EMAIL, null) ?: return null
        val password = p.getString(KEY_CUST_PASSWORD, null) ?: return null
        if (email.isBlank() || password.isBlank()) return null
        return email to password
    }

    actual fun clearCustomer() {
        prefs?.edit()?.remove(KEY_CUST_EMAIL)?.remove(KEY_CUST_PASSWORD)?.apply()
    }
}
