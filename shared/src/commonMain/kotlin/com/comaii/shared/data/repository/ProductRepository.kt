package com.comaii.shared.data.repository

import com.comaii.shared.domain.model.Category
import com.comaii.shared.domain.model.Product
import dev.gitlive.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ProductRepository(
    private val firestore: FirebaseFirestore,
) {
    private val productsCollection = firestore.collection("products")
    private val categoriesCollection = firestore.collection("categories")

    fun observeProducts(companyId: String): Flow<List<Product>> {
        return productsCollection
            .where { "companyId" equalTo companyId }
            .snapshots
            .map { snapshot ->
                snapshot.documents.map { it.data<Product>() }
                    .sortedBy { it.order }
            }
    }

    fun observeCategories(companyId: String): Flow<List<Category>> {
        return categoriesCollection
            .where { "companyId" equalTo companyId }
            .snapshots
            .map { snapshot ->
                snapshot.documents.map { it.data<Category>() }
                    .sortedBy { it.order }
            }
    }

    suspend fun addProduct(product: Product): String {
        val docRef = productsCollection.document
        val newProduct = product.copy(id = docRef.id)
        docRef.set(newProduct)
        return docRef.id
    }

    suspend fun updateProduct(product: Product) {
        productsCollection.document(product.id).set(product)
    }

    suspend fun deleteProduct(productId: String) {
        productsCollection.document(productId).delete()
    }

    suspend fun addCategory(category: Category): String {
        val docRef = categoriesCollection.document
        val newCategory = category.copy(id = docRef.id)
        docRef.set(newCategory)
        return docRef.id
    }

    suspend fun deleteCategory(categoryId: String) {
        categoriesCollection.document(categoryId).delete()
    }
}
