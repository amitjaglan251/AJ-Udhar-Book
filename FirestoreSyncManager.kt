package com.aj.udharbook.sync

import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import com.aj.udharbook.dao.CustomerDao
import com.aj.udharbook.dao.TransactionDao
import com.aj.udharbook.model.Customer
import com.aj.udharbook.model.Transaction
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.tasks.await
import kotlin.math.abs
import kotlin.random.Random

class FirestoreSyncManager(
    private val customerDao: CustomerDao,
    private val transactionDao: TransactionDao
) {

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    private val incomingListeners = mutableMapOf<String, ListenerRegistration>()

    // ==================================================
    // CURRENT USER
    // ==================================================

    fun isUserSignedIn(): Boolean = auth.currentUser != null

    fun getCurrentUserId(): String? = auth.currentUser?.uid

    // ==================================================
    // USER DOCUMENT
    // ==================================================

    private fun userDocument() =
        auth.currentUser?.uid?.let { uid ->
            firestore.collection("users").document(uid)
        }

    private fun requireUserDocument() =
        userDocument()
            ?: throw IllegalStateException("User is not signed in")

    // ==================================================
    // NORMAL CUSTOMER SYNC
    // ==================================================

    suspend fun syncCustomer(customer: Customer) {
        if (customer.id <= 0) return

        val data = hashMapOf<String, Any>(
            "id" to customer.id,
            "name" to customer.name,
            "mobile" to customer.mobile,
            "address" to customer.address,
            "createdAt" to customer.createdAt
        )

        requireUserDocument()
            .collection("customers")
            .document(customer.id.toString())
            .set(data)
            .await()
    }

    suspend fun deleteCustomer(customerId: Int) {
        if (customerId <= 0) return

        requireUserDocument()
            .collection("customers")
            .document(customerId.toString())
            .delete()
            .await()
    }

    // ==================================================
    // NORMAL TRANSACTION SYNC
    // ==================================================

    suspend fun syncTransaction(transaction: Transaction) {
        if (transaction.id <= 0) return

        val data = hashMapOf<String, Any>(
            "id" to transaction.id,
            "customerId" to transaction.customerId,
            "amount" to transaction.amount,
            "type" to transaction.type,
            "note" to transaction.note,
            "timestamp" to transaction.timestamp
        )

        requireUserDocument()
            .collection("transactions")
            .document(transaction.id.toString())
            .set(data)
            .await()

        // NEW: send the same transaction to every active linked customer.
        syncTransactionToLinkedCustomer(transaction)
    }

    suspend fun deleteTransaction(transactionId: Int) {
        if (transactionId <= 0) return

        requireUserDocument()
            .collection("transactions")
            .document(transactionId.toString())
            .delete()
            .await()

        deleteTransactionFromLinkedCustomers(transactionId)
    }

    // ==================================================
    // BULK SYNC
    // ==================================================

    suspend fun syncCustomers(customers: List<Customer>) {
        if (!isUserSignedIn()) return
        customers.filter { it.id > 0 }.forEach { syncCustomer(it) }
    }

    suspend fun syncTransactions(transactions: List<Transaction>) {
        if (!isUserSignedIn()) return
        transactions.filter { it.id > 0 }.forEach { syncTransaction(it) }
    }

    suspend fun syncAll(
        customers: List<Customer>,
        transactions: List<Transaction>
    ) {
        if (!isUserSignedIn()) return
        syncCustomers(customers)
        syncTransactions(transactions)
    }

    // ==================================================
    // APP-TO-APP: CREATE CONNECTION
    // ==================================================

    suspend fun createConnection(customerId: Int): String {
        val lenderUid =
            auth.currentUser?.uid
                ?: throw IllegalStateException("पहले Google Account से Sign In करें।")

        val customer =
            customerDao.getCustomerByIdOnce(customerId)
                ?: throw IllegalStateException("Customer नहीं मिला।")

        repeat(20) {
            val code = generateConnectionCode()
            val reference = firestore.collection("connections").document(code)
            val existing = reference.get().await()

            if (!existing.exists()) {
                val data = hashMapOf<String, Any>(
                    "code" to code,
                    "lenderUid" to lenderUid,
                    "lenderCustomerId" to customer.id,
                    "customerName" to customer.name,
                    "customerMobile" to customer.mobile,
                    "customerAddress" to customer.address,
                    "status" to "pending",
                    "createdAt" to System.currentTimeMillis()
                )

                reference.set(data).await()

                // Existing history is also copied into the shared connection.
                val existingTransactions =
                    transactionDao.getTransactionsByCustomerOnce(customer.id)

                for (transaction in existingTransactions) {
                    writeConnectionEvent(reference, customer, transaction)
                }

                return code
            }
        }

        throw IllegalStateException("Connection code generate नहीं हो सका। फिर से कोशिश करें।")
    }

    // ==================================================
    // APP-TO-APP: ACCEPT CONNECTION
    // ==================================================

    suspend fun acceptConnection(codeInput: String): String {
        val customerUid =
            auth.currentUser?.uid
                ?: throw IllegalStateException("पहले Google Account से Sign In करें।")

        val code = codeInput.trim().uppercase()
        if (code.length < 6) {
            throw IllegalArgumentException("Valid connection code डालें।")
        }

        val reference = firestore.collection("connections").document(code)
        val snapshot = reference.get().await()

        if (!snapshot.exists()) {
            throw IllegalArgumentException("Connection code नहीं मिला।")
        }

        val lenderUid = snapshot.getString("lenderUid") ?: ""
        val status = snapshot.getString("status") ?: ""

        if (lenderUid.isBlank() || lenderUid == customerUid) {
            throw IllegalArgumentException("यह connection इस account के लिए valid नहीं है।")
        }

        if (!status.equals("pending", ignoreCase = true)) {
            throw IllegalArgumentException("यह connection पहले ही accept हो चुका है।")
        }

        val customerName = snapshot.getString("customerName") ?: "Customer"
        val customerMobile = snapshot.getString("customerMobile") ?: ""
        val customerAddress = snapshot.getString("customerAddress") ?: ""

        // Update the shared connection first.
        reference.update(
            mapOf(
                "customerUid" to customerUid,
                "customerEmail" to (auth.currentUser?.email ?: ""),
                "status" to "active",
                "acceptedAt" to System.currentTimeMillis()
            )
        ).await()

        // Keep a private index under the customer account.
        requireUserDocument()
            .collection("connections")
            .document(code)
            .set(
                mapOf(
                    "code" to code,
                    "connectionId" to code,
                    "lenderUid" to lenderUid,
                    "customerName" to customerName,
                    "customerMobile" to customerMobile,
                    "createdAt" to System.currentTimeMillis()
                )
            )
            .await()

        // Create the linked customer locally if it is not already present.
        var localCustomer =
            customerDao.getCustomerByMobile(customerMobile)

        if (localCustomer == null) {
            val newId = customerDao.insert(
                Customer(
                    name = customerName,
                    mobile = customerMobile,
                    address = customerAddress
                )
            )

            localCustomer =
                customerDao.getCustomerByIdOnce(newId.toInt())
        }

        // Import current history from the connection.
        val events =
            reference.collection("events").get().await()

        if (localCustomer != null) {
            for (document in events.documents) {
                applyIncomingEvent(
                    eventId = document.id,
                    document = document,
                    localCustomer = localCustomer,
                    showNotification = false
                )
            }
        }

        startIncomingConnectionListeners()

        return customerName
    }

    suspend fun getLocalCustomers(): List<Customer> =
        customerDao.getAllCustomersOnce()

    // ==================================================
    // APP-TO-APP: LENDER -> CUSTOMER EVENT
    // ==================================================

    private suspend fun syncTransactionToLinkedCustomer(
        transaction: Transaction
    ) {
        val lenderUid = auth.currentUser?.uid ?: return
        val customer = customerDao.getCustomerByIdOnce(transaction.customerId) ?: return

        val connections = firestore
            .collection("connections")
            .whereEqualTo("lenderUid", lenderUid)
            .whereEqualTo("status", "active")
            .get()
            .await()

        for (connection in connections.documents) {
            val linkedCustomerId =
                connection.getLong("lenderCustomerId")?.toInt()

            if (linkedCustomerId == transaction.customerId) {
                writeConnectionEvent(
                    connection.reference,
                    customer,
                    transaction
                )
            }
        }
    }

    private suspend fun deleteTransactionFromLinkedCustomers(
        transactionId: Int
    ) {
        val lenderUid = auth.currentUser?.uid ?: return

        val connections = firestore
            .collection("connections")
            .whereEqualTo("lenderUid", lenderUid)
            .whereEqualTo("status", "active")
            .get()
            .await()

        for (connection in connections.documents) {
            connection.reference
                .collection("events")
                .document(transactionId.toString())
                .delete()
                .await()
        }
    }

    private suspend fun writeConnectionEvent(
        connectionReference: com.google.firebase.firestore.DocumentReference,
        customer: Customer,
        transaction: Transaction
    ) {
        val data = hashMapOf<String, Any>(
            "id" to transaction.id,
            "customerId" to transaction.customerId,
            "customerName" to customer.name,
            "customerMobile" to customer.mobile,
            "customerAddress" to customer.address,
            "amount" to transaction.amount,
            "type" to transaction.type,
            "note" to transaction.note,
            "timestamp" to transaction.timestamp
        )

        connectionReference
            .collection("events")
            .document(transaction.id.toString())
            .set(data)
            .await()
    }

    // ==================================================
    // APP-TO-APP: CUSTOMER LISTENER
    // ==================================================

    fun startIncomingConnectionListeners() {
        val uid = auth.currentUser?.uid ?: return
        val user = firestore.collection("users").document(uid)

        user.collection("connections")
            .get()
            .addOnSuccessListener { snapshot ->
                snapshot.documents.forEach { connection ->
                    attachIncomingConnectionListener(connection.id)
                }
            }
    }

    private fun attachIncomingConnectionListener(code: String) {
        if (incomingListeners.containsKey(code)) return

        val reference = firestore
            .collection("connections")
            .document(code)
            .collection("events")

        var firstSnapshot = true

        val registration = reference
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener

                for (change in snapshot.documentChanges) {
                    val customerMobile =
                        change.document.getString("customerMobile") ?: continue

                    val scope = kotlinx.coroutines.CoroutineScope(
                        kotlinx.coroutines.Dispatchers.IO
                    )

                    scope.launch {
                        val localCustomer =
                            customerDao.getCustomerByMobile(customerMobile)

                        if (localCustomer == null) return@launch

                        when (change.type) {
                            DocumentChange.Type.ADDED,
                            DocumentChange.Type.MODIFIED -> {
                                applyIncomingEvent(
                                    eventId = change.document.id,
                                    document = change.document,
                                    localCustomer = localCustomer,
                                    showNotification = !firstSnapshot
                                )
                            }

                            DocumentChange.Type.REMOVED -> {
                                transactionDao.deleteById(
                                    syncedTransactionId(change.document.id)
                                )
                            }
                        }
                    }
                }

                firstSnapshot = false
            }

        incomingListeners[code] = registration
    }

    private suspend fun applyIncomingEvent(
        eventId: String,
        document: com.google.firebase.firestore.DocumentSnapshot,
        localCustomer: Customer,
        showNotification: Boolean
    ) {
        val amount =
            document.getDouble("amount")
                ?: document.getLong("amount")?.toDouble()
                ?: return

        val type = document.getString("type") ?: "UDHAR"
        val note = document.getString("note") ?: ""
        val timestamp =
            document.getLong("timestamp") ?: System.currentTimeMillis()

        transactionDao.insert(
            Transaction(
                id = syncedTransactionId(eventId),
                customerId = localCustomer.id,
                amount = amount,
                type = type,
                note = note,
                timestamp = timestamp
            )
        )

        if (showNotification) {
            showTransactionNotification(
                customerName = localCustomer.name,
                amount = amount,
                type = type
            )
        }
    }

    private fun syncedTransactionId(eventId: String): Int {
        val hash = eventId.hashCode()
        val positive = if (hash == Int.MIN_VALUE) Int.MAX_VALUE else abs(hash)
        return -positive.coerceAtLeast(1)
    }

    fun stopIncomingConnectionListeners() {
        incomingListeners.values.forEach { it.remove() }
        incomingListeners.clear()
    }

    // ==================================================
    // LOCAL NOTIFICATION
    // ==================================================

    private fun showTransactionNotification(
        customerName: String,
        amount: Double,
        type: String
    ) {
        val context = FirebaseApp.getInstance().applicationContext
        val channelId = "aj_udharbook_connections"
        val manager = context.getSystemService(NotificationManager::class.java)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(
                    channelId,
                    "AJ Udhar Book Updates",
                    NotificationManager.IMPORTANCE_HIGH
                )
            )
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val action =
            if (type.equals("PAYMENT", ignoreCase = true)) "Payment" else "Udhar"

        val text =
            "$customerName: ₹${String.format("%.2f", amount)} $action update"

        val notification =
            android.app.Notification.Builder(context, channelId)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("AJ Udhar Book")
                .setContentText(text)
                .setAutoCancel(true)
                .build()

        NotificationManagerCompat.from(context).notify(
            (System.currentTimeMillis() % Int.MAX_VALUE).toInt(),
            notification
        )
    }

    // ==================================================
    // RESTORE CLOUD DATA
    // ==================================================

    suspend fun restoreCloudToLocal() {
        if (!isUserSignedIn()) {
            throw IllegalStateException("User is not signed in")
        }

        val user = requireUserDocument()

        val customerSnapshot =
            user.collection("customers").get().await()

        val cloudCustomers =
            customerSnapshot.documents.mapNotNull { document ->
                val id = document.getLong("id")?.toInt()
                    ?: document.id.toIntOrNull()
                    ?: return@mapNotNull null

                Customer(
                    id = id,
                    name = document.getString("name") ?: "",
                    mobile = document.getString("mobile") ?: "",
                    address = document.getString("address") ?: "",
                    createdAt = document.getLong("createdAt")
                        ?: System.currentTimeMillis()
                )
            }

        val transactionSnapshot =
            user.collection("transactions").get().await()

        val cloudTransactions =
            transactionSnapshot.documents.mapNotNull { document ->
                val id = document.getLong("id")?.toInt()
                    ?: document.id.toIntOrNull()
                    ?: return@mapNotNull null

                val customerId = document.getLong("customerId")?.toInt()
                    ?: return@mapNotNull null

                Transaction(
                    id = id,
                    customerId = customerId,
                    amount = document.getDouble("amount")
                        ?: document.getLong("amount")?.toDouble()
                        ?: 0.0,
                    type = document.getString("type") ?: "UDHAR",
                    note = document.getString("note") ?: "",
                    timestamp = document.getLong("timestamp")
                        ?: System.currentTimeMillis()
                )
            }

        val localCustomers = customerDao.getAllCustomersOnce()
        val localTransactions = transactionDao.getAllTransactionsOnce()

        if (localCustomers.isEmpty() && cloudCustomers.isNotEmpty()) {
            customerDao.insertAll(cloudCustomers)
        }

        if (localTransactions.isEmpty() && cloudTransactions.isNotEmpty()) {
            transactionDao.insertAll(cloudTransactions)
        }

        // Start listening for linked-customer updates after login/restore.
        startIncomingConnectionListeners()
    }

    // ==================================================
    // CLEAR LOCAL DATA
    // ==================================================

    suspend fun clearLocalData() {
        stopIncomingConnectionListeners()
        transactionDao.deleteAll()
        customerDao.deleteAll()
    }

    // ==================================================
    // HELPERS
    // ==================================================

    private fun generateConnectionCode(): String {
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        return buildString {
            repeat(8) {
                append(chars[Random.nextInt(chars.length)])
            }
        }
    }
}

private suspend fun TransactionDao.getTransactionsByCustomerOnce(
    customerId: Int
): List<Transaction> {
    return getAllTransactionsOnce().filter { it.customerId == customerId }
}
