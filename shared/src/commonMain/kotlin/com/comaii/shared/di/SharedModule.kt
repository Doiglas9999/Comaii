package com.comaii.shared.di

import com.comaii.shared.data.repository.CompanyRepository
import com.comaii.shared.data.repository.OrderRepository
import com.comaii.shared.data.repository.ProductRepository
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth
import dev.gitlive.firebase.firestore.firestore
import dev.gitlive.firebase.storage.storage
import org.koin.dsl.module

val sharedModule = module {
    // Firebase
    single { Firebase.firestore }
    single { Firebase.auth }
    single { Firebase.storage }

    // Repositories
    single { CompanyRepository(get()) }
    single { ProductRepository(get()) }
    single { OrderRepository(get()) }
}
