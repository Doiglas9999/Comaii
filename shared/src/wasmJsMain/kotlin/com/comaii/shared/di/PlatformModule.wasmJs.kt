package com.comaii.shared.di

import com.comaii.shared.data.firebase.FirebaseService
import com.comaii.shared.data.firebase.WasmFirebaseService
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun platformModule(): Module = module {
    single<FirebaseService> { WasmFirebaseService() }
}
