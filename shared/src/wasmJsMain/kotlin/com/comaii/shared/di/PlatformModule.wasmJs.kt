package com.comaii.shared.di

import com.comaii.shared.data.firebase.FirebaseService
import com.comaii.shared.data.firebase.WasmFirebaseService
import io.ktor.client.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun platformModule(): Module = module {
    single<HttpClient> {
        HttpClient {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }
    }
    single<FirebaseService> { WasmFirebaseService(get()) }
}
