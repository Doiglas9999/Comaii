package com.comaii.app.di

import com.comaii.app.ui.screens.admin.AdminDashboardScreenModel
import com.comaii.app.ui.screens.admin.categories.CategoriesScreenModel
import com.comaii.app.ui.screens.admin.orders.OrdersDashboardScreenModel
import com.comaii.app.ui.screens.admin.products.ProductsScreenModel
import com.comaii.app.ui.screens.admin.settings.StoreSettingsScreenModel
import com.comaii.app.ui.screens.auth.AuthScreenModel
import com.comaii.app.ui.screens.customer.CustomerOrdersScreenModel
import com.comaii.app.ui.screens.store.StoreScreenModel
import com.comaii.shared.di.platformModule
import com.comaii.shared.di.sharedModule
import org.koin.dsl.module

val appModule = module {
    // ScreenModels
    factory { AuthScreenModel(get()) }
    factory { params -> AdminDashboardScreenModel(params.get(), get()) }
    factory { params -> ProductsScreenModel(params.get(), get()) }
    factory { params -> StoreScreenModel(params.get(), get()) }
    factory { params -> StoreSettingsScreenModel(params.get(), get()) }
    factory { params -> CategoriesScreenModel(params.get(), get()) }
    factory { params -> OrdersDashboardScreenModel(params.get(), get()) }
    factory { (companyId: String, customerId: String) -> CustomerOrdersScreenModel(companyId, customerId, get()) }
}

val allModules = listOf(platformModule(), sharedModule, appModule)
