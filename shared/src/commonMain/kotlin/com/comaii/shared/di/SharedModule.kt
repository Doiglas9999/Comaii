package com.comaii.shared.di

import com.comaii.shared.data.firebase.FirebaseService
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * Módulo Koin esperado por cada plataforma.
 * Cada plataforma fornece a implementação de FirebaseService.
 */
expect fun platformModule(): Module

val sharedModule = module {
    // O FirebaseService é fornecido pelo platformModule()
}
