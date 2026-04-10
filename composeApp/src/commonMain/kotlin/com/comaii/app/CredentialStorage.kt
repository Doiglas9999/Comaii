package com.comaii.app

/** Armazenamento persistente de credenciais por plataforma */
expect object CredentialStorage {
    fun save(email: String, password: String)
    fun load(): Pair<String, String>?
    fun clear()
}
