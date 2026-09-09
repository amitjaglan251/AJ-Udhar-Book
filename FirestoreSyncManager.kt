package com.aj.udharbook.sync

import com.aj.udharbook.dao.CustomerDao
import com.aj.udharbook.dao.TransactionDao
import com.aj.udharbook.model.Customer
import com.aj.udharbook.model.Transaction
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID

/** Firebase sync layer. Shared ledgers no longer use the old 6-digit approval flow. */
class FirestoreSyncManager(
    private val customerDao: CustomerDao,
    private val transactionDao: TransactionDao
) {
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val sharedListeners = mutableMapOf<String, ListenerRegistration>()
    private val syncScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun isUserSignedIn(): Boolean = auth.currentUser != null
    fun getCurrentUserId(): String? = auth.currentUser?.uid
    suspend fun getCustomerByIdOnce(customerId: Int): Customer? = customerDao.getCustomerByIdOnce(customerId)

    private fun userDocument() = auth.currentUser?.uid?.let { firestore.collection("users").document(it) }
    private fun requireUserDocument() = userDocument() ?: throw IllegalStateException("User is not signed in")

    // ---------------- Existing private cloud sync ----------------
    suspend fun syncCustomer(customer: Customer) {
        if (customer.id <= 0) return
        requireUserDocument().collection("customers").document(customer.id.toString()).set(
            hashMapOf<String, Any>(
                "id" to customer.id, "name" to customer.name, "mobile" to customer.mobile,
                "address" to customer.address, "createdAt" to customer.createdAt,
                "sharedLedgerId" to customer.sharedLedgerId
            )
        ).await()
    }

    suspend fun deleteCustomer(customerId: Int) {
        if (customerId <= 0) return
        requireUserDocument().collection("customers").document(customerId.toString()).delete().await()
    }

    suspend fun syncTransaction(transaction: Transaction) {
        if (transaction.id <= 0) return
        requireUserDocument().collection("transactions").document(transaction.id.toString()).set(
            hashMapOf<String, Any>(
                "id" to transaction.id, "customerId" to transaction.customerId,
                "amount" to transaction.amount, "type" to transaction.type,
                "note" to transaction.note, "timestamp" to transaction.timestamp,
                "syncKey" to transaction.syncKey
            )
        ).await()
    }

    suspend fun deleteTransaction(transactionId: Int) {
        if (transactionId <= 0) return
        requireUserDocument().collection("transactions").document(transactionId.toString()).delete().await()
    }

    suspend fun syncCustomers(customers: List<Customer>) {
        if (!isUserSignedIn()) return
        customers.filter { it.id > 0 }.forEach { syncCustomer(it) }
    }

    suspend fun syncTransactions(transactions: List<Transaction>) {
        if (!isUserSignedIn()) return
        transactions.filter { it.id > 0 }.forEach { syncTransaction(it) }
    }

    suspend fun syncAll(customers: List<Customer>, transactions: List<Transaction>) {
        if (!isUserSignedIn()) return
        syncCustomers(customers)
        syncTransactions(transactions)
        syncAllSharedLedgers()
    }

    suspend fun restoreCloudToLocal() {
        if (!isUserSignedIn()) throw IllegalStateException("User is not signed in")
        val user = requireUserDocument()

        val cloudCustomers = user.collection("customers").get().await().documents.mapNotNull { document ->
            val id = document.getLong("id")?.toInt() ?: document.id.toIntOrNull() ?: return@mapNotNull null
            Customer(
                id = id,
                name = document.getString("name") ?: "",
                mobile = document.getString("mobile") ?: "",
                address = document.getString("address") ?: "",
                createdAt = document.getLong("createdAt") ?: System.currentTimeMillis(),
                sharedLedgerId = document.getString("sharedLedgerId") ?: ""
            )
        }

        val cloudTransactions = user.collection("transactions").get().await().documents.mapNotNull { document ->
            val id = document.getLong("id")?.toInt() ?: document.id.toIntOrNull() ?: return@mapNotNull null
            val customerId = document.getLong("customerId")?.toInt() ?: return@mapNotNull null
            Transaction(
                id = id,
                customerId = customerId,
                amount = document.getDouble("amount") ?: document.getLong("amount")?.toDouble() ?: 0.0,
                type = document.getString("type") ?: "UDHAR",
                note = document.getString("note") ?: "",
                timestamp = document.getLong("timestamp") ?: System.currentTimeMillis(),
                syncKey = document.getString("syncKey") ?: ""
            )
        }

        if (customerDao.getAllCustomersOnce().isEmpty() && cloudCustomers.isNotEmpty()) customerDao.insertAll(cloudCustomers)
        if (transactionDao.getAllTransactionsOnce().isEmpty() && cloudTransactions.isNotEmpty()) transactionDao.insertAll(cloudTransactions)

        customerDao.getAllCustomersOnce().filter { isNewLedgerId(it.sharedLedgerId) }.forEach { customer ->
            try {
                syncSharedLedgerToLocal(customer.sharedLedgerId, customer.id)
                listenToSharedLedger(customer.sharedLedgerId, customer.id)
            } catch (_: Exception) { }
        }
    }

    // ---------------- Secure OkCredit-style shared ledger ----------------
    suspend fun createSharedLedger(customer: Customer): SharedLedgerInvite {
        if (!isUserSignedIn()) throw IllegalStateException("Login required")
        if (customer.id <= 0) throw IllegalArgumentException("Customer is not saved yet")
        val uid = getCurrentUserId() ?: throw IllegalStateException("Login required")

        val currentId = customer.sharedLedgerId.trim()
        val ledgerId = if (isNewLedgerId(currentId)) {
            val existing = firestore.collection("sharedLedgers").document(currentId).get().await()
            if (existing.exists() && existing.getString("ownerUid") == uid) currentId
            else UUID.randomUUID().toString().replace("-", "")
        } else UUID.randomUUID().toString().replace("-", "")

        val ledgerRef = firestore.collection("sharedLedgers").document(ledgerId)
        val existingLedger = ledgerRef.get().await()
        if (!existingLedger.exists()) {
            ledgerRef.set(hashMapOf<String, Any>(
                "ownerUid" to uid,
                "ownerCustomerId" to customer.id,
                "customerName" to customer.name,
                "mobile" to customer.mobile.filter(Char::isDigit).takeLast(10),
                "address" to customer.address,
                "createdAt" to System.currentTimeMillis()
            )).await()
        } else if (existingLedger.getString("ownerUid") != uid) {
            throw IllegalStateException("Only the ledger owner can manage this ledger")
        }

        ledgerRef.collection("members").document(uid).set(hashMapOf<String, Any>(
            "uid" to uid, "role" to "OWNER", "status" to "ACTIVE", "joinedAt" to System.currentTimeMillis()
        )).await()

        val token = UUID.randomUUID().toString().replace("-", "") +
            UUID.randomUUID().toString().replace("-", "").take(16)
        firestore.collection("sharedLedgerInvites").document(token).set(hashMapOf<String, Any>(
            "ledgerId" to ledgerId, "ownerUid" to uid, "role" to "CUSTOMER", "createdAt" to System.currentTimeMillis()
        )).await()

        val updatedCustomer = customer.copy(sharedLedgerId = ledgerId)
        customerDao.update(updatedCustomer)
        syncCustomer(updatedCustomer)
        publishCustomerTransactions(updatedCustomer)
        listenToSharedLedger(ledgerId, customer.id)

        return SharedLedgerInvite(ledgerId, token, customer.name, customer.mobile)
    }

    suspend fun joinSharedLedger(tokenInput: String): Customer {
        if (!isUserSignedIn()) throw IllegalStateException("Login required")
        val token = tokenInput.trim()
        if (token.length < 40) throw IllegalArgumentException("Invalid or incomplete invite token")
        val uid = getCurrentUserId() ?: throw IllegalStateException("Login required")

        val invite = firestore.collection("sharedLedgerInvites").document(token).get().await()
        if (!invite.exists()) throw IllegalArgumentException("Invite expired or invalid")
        val ledgerId = invite.getString("ledgerId") ?: throw IllegalArgumentException("Invite is incomplete")

        val ledgerRef = firestore.collection("sharedLedgers").document(ledgerId)
        val ledger = ledgerRef.get().await()
        if (!ledger.exists()) throw IllegalArgumentException("Shared ledger no longer exists")

        ledgerRef.collection("members").document(uid).set(hashMapOf<String, Any>(
            "uid" to uid,
            "role" to "CUSTOMER",
            "status" to "ACTIVE",
            "joinedAt" to System.currentTimeMillis(),
            "ledgerId" to ledgerId,
            "inviteToken" to token
        )).await()

        val name = ledger.getString("customerName") ?: "Shared Customer"
        val mobile = ledger.getString("mobile") ?: ""
        val address = ledger.getString("address") ?: ""

        val existingCustomer = customerDao.getCustomerBySharedLedgerOnce(ledgerId)
            ?: if (mobile.isNotBlank()) customerDao.getCustomerByMobileOnce(mobile) else null

        val joinedCustomer = if (existingCustomer == null) {
            val localId = customerDao.insert(Customer(
                name = name, mobile = mobile, address = address, sharedLedgerId = ledgerId
            )).toInt()
            customerDao.getCustomerByIdOnce(localId)
                ?: throw IllegalStateException("Could not create local customer")
        } else if (existingCustomer.sharedLedgerId != ledgerId) {
            val updated = existingCustomer.copy(
                name = if (existingCustomer.name.isBlank()) name else existingCustomer.name,
                mobile = if (existingCustomer.mobile.isBlank()) mobile else existingCustomer.mobile,
                address = if (existingCustomer.address.isBlank()) address else existingCustomer.address,
                sharedLedgerId = ledgerId
            )
            customerDao.update(updated)
            updated
        } else {
            existingCustomer
        }

        syncSharedLedgerToLocal(ledgerId, joinedCustomer.id)
        listenToSharedLedger(ledgerId, joinedCustomer.id)
        return joinedCustomer
    }

    suspend fun getSharedLedgerInfo(customer: Customer): SharedLedgerInfo? {
        val ledgerId = customer.sharedLedgerId.trim()
        if (!isNewLedgerId(ledgerId) || !isUserSignedIn()) return null
        val uid = getCurrentUserId() ?: return null
        val ledger = firestore.collection("sharedLedgers").document(ledgerId).get().await()
        if (!ledger.exists()) return null
        val ownerUid = ledger.getString("ownerUid") ?: return null
        val member = ledger.collection("members").document(uid).get().await()
        return SharedLedgerInfo(
            ledgerId = ledgerId,
            customerName = ledger.getString("customerName") ?: customer.name,
            mobile = ledger.getString("mobile") ?: customer.mobile,
            ownerUid = ownerUid,
            role = member.getString("role") ?: if (ownerUid == uid) "OWNER" else "CUSTOMER",
            active = member.getString("status") == "ACTIVE"
        )
    }

    suspend fun syncAllSharedLedgers() {
        if (!isUserSignedIn()) return
        customerDao.getAllCustomersOnce().filter { isNewLedgerId(it.sharedLedgerId) }.forEach { customer ->
            try {
                publishCustomerTransactions(customer)
                listenToSharedLedger(customer.sharedLedgerId, customer.id)
            } catch (_: Exception) { }
        }
    }

    suspend fun revokeInvite(token: String) {
        if (!isUserSignedIn() || token.isBlank()) return
        val uid = getCurrentUserId() ?: return
        val ref = firestore.collection("sharedLedgerInvites").document(token)
        val snapshot = ref.get().await()
        if (snapshot.getString("ownerUid") != uid) throw IllegalStateException("Only the ledger owner can revoke this invite")
        ref.delete().await()
    }

    // ---------------- Shared transaction mirroring ----------------
    suspend fun syncSharedLedgerTransaction(transaction: Transaction) {
        if (transaction.syncKey.isBlank() || !isUserSignedIn()) return
        val customer = customerDao.getCustomerByIdOnce(transaction.customerId) ?: return
        val ledgerId = customer.sharedLedgerId.trim()
        if (!isNewLedgerId(ledgerId)) return
        val uid = getCurrentUserId() ?: return
        val member = firestore.collection("sharedLedgers").document(ledgerId)
            .collection("members").document(uid).get().await()
        if (!member.exists() || member.getString("status") != "ACTIVE") return

        firestore.collection("sharedLedgers").document(ledgerId).collection("transactions")
            .document(transaction.syncKey).set(hashMapOf<String, Any>(
                "syncKey" to transaction.syncKey,
                "amount" to transaction.amount,
                "type" to transaction.type,
                "note" to transaction.note,
                "timestamp" to transaction.timestamp,
                "createdBy" to uid
            )).await()
    }

    suspend fun deleteSharedLedgerTransaction(transaction: Transaction) {
        if (transaction.syncKey.isBlank() || !isUserSignedIn()) return
        val customer = customerDao.getCustomerByIdOnce(transaction.customerId) ?: return
        val ledgerId = customer.sharedLedgerId.trim()
        if (!isNewLedgerId(ledgerId)) return
        firestore.collection("sharedLedgers").document(ledgerId).collection("transactions")
            .document(transaction.syncKey).delete().await()
    }

    private suspend fun publishCustomerTransactions(customer: Customer) {
        val ledgerId = customer.sharedLedgerId.trim()
        if (!isNewLedgerId(ledgerId) || !isUserSignedIn()) return
        val uid = getCurrentUserId() ?: return

        transactionDao.getTransactionsByCustomerOnce(customer.id).forEach { transaction ->
            val syncKey = if (transaction.syncKey.isBlank()) UUID.randomUUID().toString() else transaction.syncKey
            val shared = transaction.copy(syncKey = syncKey)
            if (transaction.syncKey.isBlank()) transactionDao.update(shared)
            firestore.collection("sharedLedgers").document(ledgerId).collection("transactions")
                .document(syncKey).set(hashMapOf<String, Any>(
                    "syncKey" to syncKey,
                    "amount" to shared.amount,
                    "type" to shared.type,
                    "note" to shared.note,
                    "timestamp" to shared.timestamp,
                    "createdBy" to uid
                )).await()
        }
    }

    suspend fun syncSharedLedgerToLocal(ledgerId: String, localCustomerId: Int) {
        if (!isNewLedgerId(ledgerId)) return
        val snapshot = firestore.collection("sharedLedgers").document(ledgerId)
            .collection("transactions").get().await()
        snapshot.documents.forEach { upsertSharedTransaction(it, localCustomerId) }
    }

    private suspend fun upsertSharedTransaction(
        doc: com.google.firebase.firestore.DocumentSnapshot,
        localCustomerId: Int
    ) {
        val syncKey = doc.getString("syncKey") ?: doc.id
        if (syncKey.isBlank()) return
        val existing = transactionDao.getTransactionBySyncKey(syncKey)
        transactionDao.insert(Transaction(
            id = existing?.id ?: 0,
            customerId = localCustomerId,
            amount = doc.getDouble("amount") ?: doc.getLong("amount")?.toDouble() ?: 0.0,
            type = doc.getString("type") ?: "UDHAR",
            note = doc.getString("note") ?: "",
            timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis(),
            syncKey = syncKey
        ))
    }

    fun listenToSharedLedger(ledgerId: String, localCustomerId: Int) {
        if (!isUserSignedIn() || !isNewLedgerId(ledgerId)) return
        sharedListeners.remove(ledgerId)?.remove()
        sharedListeners[ledgerId] = firestore.collection("sharedLedgers").document(ledgerId)
            .collection("transactions").addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener
                syncScope.launch {
                    snapshot.documentChanges.forEach { change ->
                        val doc = change.document
                        val syncKey = doc.getString("syncKey") ?: doc.id
                        when (change.type) {
                            DocumentChange.Type.ADDED, DocumentChange.Type.MODIFIED -> {
                                try { upsertSharedTransaction(doc, localCustomerId) } catch (_: Exception) { }
                            }
                            DocumentChange.Type.REMOVED -> {
                                try { transactionDao.deleteBySyncKey(syncKey) } catch (_: Exception) { }
                            }
                        }
                    }
                }
            }
    }

    fun stopListening(ledgerId: String) { sharedListeners.remove(ledgerId)?.remove() }

    fun stopAllSharedListeners() {
        sharedListeners.values.forEach { it.remove() }
        sharedListeners.clear()
    }

    suspend fun clearLocalData() {
        stopAllSharedListeners()
        transactionDao.deleteAll()
        customerDao.deleteAll()
    }

    private fun isNewLedgerId(value: String): Boolean {
        val normalized = value.trim()
        return normalized.length >= 24 && normalized.all { it.isLetterOrDigit() }
    }
}

data class SharedLedgerInvite(
    val ledgerId: String,
    val token: String,
    val customerName: String,
    val mobile: String
)

data class SharedLedgerInfo(
    val ledgerId: String,
    val customerName: String,
    val mobile: String,
    val ownerUid: String,
    val role: String,
    val active: Boolean
)
