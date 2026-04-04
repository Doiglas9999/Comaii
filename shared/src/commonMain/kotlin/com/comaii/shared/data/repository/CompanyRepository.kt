package com.comaii.shared.data.repository

import com.comaii.shared.domain.model.Company
import dev.gitlive.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class CompanyRepository(
    private val firestore: FirebaseFirestore,
) {
    private val collection = firestore.collection("companies")

    suspend fun getCompanyById(id: String): Company? {
        return try {
            val doc = collection.document(id).get()
            if (doc.exists) doc.data<Company>() else null
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getCompanyBySlug(slug: String): Company? {
        return try {
            val query = collection.where { "slug" equalTo slug }.get()
            query.documents.firstOrNull()?.data<Company>()
        } catch (e: Exception) {
            null
        }
    }

    suspend fun createCompany(company: Company): String {
        val docRef = collection.document
        val newCompany = company.copy(id = docRef.id)
        docRef.set(newCompany)
        return docRef.id
    }

    suspend fun updateCompany(company: Company) {
        collection.document(company.id).set(company)
    }

    fun observeCompany(id: String): Flow<Company?> {
        return collection.document(id).snapshots.map { snapshot ->
            if (snapshot.exists) snapshot.data<Company>() else null
        }
    }
}
