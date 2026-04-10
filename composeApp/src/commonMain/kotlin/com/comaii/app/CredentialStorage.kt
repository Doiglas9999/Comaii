package com.comaii.app

/** Armazenamento persistente de credenciais por plataforma */
expect object CredentialStorage {
    // Admin credentials
    fun save(email: String, password: String)
    fun load(): Pair<String, String>?
    fun clear()

    // Customer (store) credentials — stored under separate keys
    fun saveCustomer(email: String, password: String)
    fun loadCustomer(): Pair<String, String>?
    fun clearCustomer()
}
