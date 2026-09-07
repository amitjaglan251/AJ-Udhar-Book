package com.aj.udharbook.sync

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.tasks.await

class JoinRequestObserver {
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val listeners = mutableMapOf<String, ListenerRegistration>()
    private val requestsByLedger = mutableMapOf<String, List<JoinRequestNotification>>()

    suspend fun start(
        ledgerCodes: List<String>,
        onChanged: (List<JoinRequestNotification>) -> Unit
    ) {
        stop()
        val uid = auth.currentUser?.uid ?: return
        ledgerCodes.filter { SharedLedgerSecurity.isValidCode(it) }.distinct().forEach { code ->
            val ledger = firestore.collection("sharedLedgers").document(code).get().await()
            if (ledger.getString("ownerUid") != uid) return@forEach

            val registration = firestore.collection("sharedLedgers").document(code)
                .collection("joinRequests")
                .addSnapshotListener { snapshot, error ->
                    if (error != null || snapshot == null) return@addSnapshotListener
                    requestsByLedger[code] = snapshot.documents.map { document ->
                        JoinRequestNotification(
                            requesterUid = document.getString("requesterUid") ?: document.id,
                            ledgerCode = code,
                            status = document.getString("status") ?: SharedLedgerSecurity.PENDING
                        )
                    }
                    val allRequests = requestsByLedger.values.flatten()
                    onChanged(allRequests)
                }
            listeners[code] = registration
        }
    }

    fun stop() {
        listeners.values.forEach { it.remove() }
        listeners.clear()
        requestsByLedger.clear()
    }
}
