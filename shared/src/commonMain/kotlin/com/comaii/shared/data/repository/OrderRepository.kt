package com.comaii.shared.data.repository

import com.comaii.shared.domain.model.Order
import com.comaii.shared.domain.model.OrderStatus
import dev.gitlive.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class OrderRepository(
    private val firestore: FirebaseFirestore,
) {
    private val collection = firestore.collection("orders")

    suspend fun createOrder(order: Order): String {
        val docRef = collection.document
        val newOrder = order.copy(
            id = docRef.id,
            createdAt = kotlinx.datetime.Clock.System.now().toEpochMilliseconds(),
        )
        docRef.set(newOrder)
        return docRef.id
    }

    fun observeOrders(companyId: String): Flow<List<Order>> {
        return collection
            .where { "companyId" equalTo companyId }
            .snapshots
            .map { snapshot ->
                snapshot.documents.map { it.data<Order>() }
                    .sortedByDescending { it.createdAt }
            }
    }

    suspend fun updateOrderStatus(orderId: String, status: OrderStatus) {
        collection.document(orderId).update("status" to status.name)
    }
}
