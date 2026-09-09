package com.aj.udharbook.ui.sharedledger

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aj.udharbook.model.Customer
import com.aj.udharbook.sync.FirestoreSyncManager
import kotlinx.coroutines.launch

/** New secure Shared Ledger UI: owner shares a customer ledger, customer joins directly. */
@Composable
fun SharedLedgerScreen(
    customers: List<Customer>,
    firestoreSyncManager: FirestoreSyncManager,
    onJoined: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var token by remember { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    var createdInvite by remember { mutableStateOf<String?>(null) }

    fun copyToken(value: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("AJ Udhar Book Invite", value))
        message = "Secure invite token copied."
        error = null
    }

    fun shareInvite(customer: Customer) {
        loading = true
        message = null
        error = null
        scope.launch {
            try {
                val invite = firestoreSyncManager.createSharedLedger(customer)
                createdInvite = invite.token
                val sms = "AJ Udhar Book: ${customer.name} ka shared ledger join karne ke liye AJ Udhar Book app me Secure Invite Token paste karein:\n${invite.token}"
                try {
                    context.startActivity(
                        Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:${customer.mobile.trim()}"))
                            .putExtra("sms_body", sms)
                    )
                    message = "Secure invite created and SMS composer opened."
                } catch (_: Exception) {
                    copyToken(invite.token)
                    message = "Secure invite created. SMS app नहीं मिली, token copy कर दिया गया।"
                }
            } catch (e: Exception) {
                error = e.message ?: "Could not create shared ledger"
            } finally {
                loading = false
            }
        }
    }

    Scaffold(topBar = { TopAppBar(title = { Text("🔗 Shared Ledger") }) }) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text("Same Ledger • Real-time Sync", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(
                    "Owner और customer एक ही Debit/Credit history और balance देखेंगे। " +
                        "पुराना 6-digit code और approval queue अब इस्तेमाल नहीं होता।",
                    modifier = Modifier.padding(top = 6.dp)
                )
            }

            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("👑 Owner — Customer Ledger Share", fontWeight = FontWeight.Bold)
                        Text("जिस customer का ledger share करना है, उसके सामने Share दबाएँ।")
                    }
                }
            }

            if (customers.isEmpty()) {
                item { Text("पहले customer add करें।") }
            } else {
                items(customers) { customer ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(customer.name, fontWeight = FontWeight.Bold)
                            Text(if (customer.mobile.isBlank()) "Mobile: —" else "Mobile: ${customer.mobile}")
                            if (customer.sharedLedgerId.isNotBlank()) {
                                Text("Shared ledger already linked", color = MaterialTheme.colorScheme.primary)
                            }
                            Button(
                                onClick = { shareInvite(customer) },
                                enabled = !loading,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(if (customer.sharedLedgerId.isBlank()) "Create & Share Secure Invite" else "Create New Invite")
                            }
                        }
                    }
                }
            }

            createdInvite?.let { inviteToken ->
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Secure Invite Token", fontWeight = FontWeight.Bold)
                            Text(inviteToken)
                            OutlinedButton(onClick = { copyToken(inviteToken) }, modifier = Modifier.fillMaxWidth()) {
                                Text("Copy Token")
                            }
                        }
                    }
                }
            }

            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("👤 Customer — Join Ledger", fontWeight = FontWeight.Bold)
                        Text("Owner से मिला Secure Invite Token यहाँ paste करें। Join होते ही पूरा ledger sync होगा।")
                        OutlinedTextField(
                            value = token,
                            onValueChange = {
                                token = it.filter(Char::isLetterOrDigit).take(80)
                                message = null
                                error = null
                            },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Secure Invite Token") },
                            singleLine = true
                        )
                        Button(
                            onClick = {
                                loading = true
                                message = null
                                error = null
                                scope.launch {
                                    try {
                                        firestoreSyncManager.joinSharedLedger(token)
                                        message = "Ledger joined successfully. Transactions will sync in real time."
                                        onJoined()
                                    } catch (e: Exception) {
                                        error = e.message ?: "Join failed"
                                    } finally {
                                        loading = false
                                    }
                                }
                            },
                            enabled = token.length >= 40 && !loading,
                            modifier = Modifier.fillMaxWidth()
                        ) { Text(if (loading) "Please wait…" else "Join Shared Ledger") }
                    }
                }
            }

            if (message != null) item { Text(message ?: "", color = MaterialTheme.colorScheme.primary) }
            if (error != null) item { Text(error ?: "", color = MaterialTheme.colorScheme.error) }

            item {
                Text(
                    "Balance = Debit (Udhar) − Credit (Payment). Room remains the local/offline source and Firestore mirrors the shared ledger when online.",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            item { Button(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Back") } }
        }
    }
}
