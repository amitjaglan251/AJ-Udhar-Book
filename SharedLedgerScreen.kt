package com.aj.udharbook.ui.sharedledger

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aj.udharbook.sync.FirestoreSyncManager
import com.aj.udharbook.sync.SharedJoinRequest
import com.aj.udharbook.sync.SharedLedgerSecurity
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.launch

@Composable
fun SharedLedgerScreen(
    firestoreSyncManager: FirestoreSyncManager,
    onJoined: () -> Unit,
    onBack: () -> Unit
) {
    var code by remember { mutableStateOf("") }
    var status by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    var requests by remember { mutableStateOf<List<SharedJoinRequest>>(emptyList()) }
    val scope = rememberCoroutineScope()

    fun friendlyError(e: Exception, action: String): String {
        return if (e is FirebaseFirestoreException && e.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
            "Firestore permission denied. Firebase Console me latest firestore.rules deploy karein, aur ensure karein ki user login hai."
        } else {
            e.message ?: "$action failed"
        }
    }

    fun refreshRequests() {
        scope.launch {
            try {
                requests = firestoreSyncManager.getOwnedJoinRequests()
            } catch (e: Exception) {
                error = friendlyError(e, "Requests load")
            }
        }
    }

    LaunchedEffect(Unit) { refreshRequests() }

    Scaffold(topBar = { TopAppBar(title = { Text("🔗 Shared Ledger") }) }) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Join Customer Ledger", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text("Customer ke SMS me mila 6-digit code yahan enter karein. Owner approval ke baad hi ledger access milega.")
                    OutlinedTextField(
                        value = code,
                        onValueChange = { value ->
                            code = value.filter(Char::isDigit).take(6)
                            error = null
                            status = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("6-digit Share Code") },
                        singleLine = true
                    )
                    Button(
                        onClick = {
                            loading = true
                            error = null
                            scope.launch {
                                try {
                                    status = firestoreSyncManager.requestJoinShareCode(code)
                                    if (status == SharedLedgerSecurity.APPROVED) {
                                        firestoreSyncManager.completeApprovedJoin(code)
                                        onJoined()
                                    }
                                } catch (e: Exception) {
                                    error = friendlyError(e, "Join request")
                                } finally {
                                    loading = false
                                }
                            }
                        },
                        enabled = code.length == 6 && !loading,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (loading) CircularProgressIndicator(modifier = Modifier.padding(2.dp)) else Text("Request Join")
                    }
                    OutlinedButton(
                        onClick = {
                            loading = true
                            error = null
                            scope.launch {
                                try {
                                    status = firestoreSyncManager.getJoinRequestStatus(code)
                                    when (status) {
                                        SharedLedgerSecurity.APPROVED -> {
                                            firestoreSyncManager.completeApprovedJoin(code)
                                            onJoined()
                                        }
                                        SharedLedgerSecurity.REJECTED -> error = "Owner ne request reject kar di hai."
                                        null -> error = "Is code ke liye koi request nahi mili."
                                    }
                                } catch (e: Exception) {
                                    error = friendlyError(e, "Approval check")
                                } finally {
                                    loading = false
                                }
                            }
                        },
                        enabled = code.length == 6 && !loading,
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Check Approval") }
                    if (status != null) Text("Status: $status")
                    if (error != null) Text(error ?: "", color = MaterialTheme.colorScheme.error)
                }
            }

            item {
                Text("Owner Approval Requests", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                if (requests.isEmpty()) Text("No pending requests.")
            }

            items(requests.filter { it.status == SharedLedgerSecurity.PENDING }) { request ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(request.requesterName, fontWeight = FontWeight.Bold)
                        Text("Ledger: ${request.ledgerCode}")
                        Text("Requester: ${request.requesterUid.take(12)}…")
                        Button(
                            onClick = {
                                scope.launch {
                                    try {
                                        firestoreSyncManager.approveJoinRequest(request)
                                        refreshRequests()
                                    } catch (e: Exception) {
                                        error = friendlyError(e, "Approval")
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Approve") }
                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    try {
                                        firestoreSyncManager.rejectJoinRequest(request)
                                        refreshRequests()
                                    } catch (e: Exception) {
                                        error = friendlyError(e, "Rejection")
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Reject") }
                    }
                }
            }

            item { Button(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Back") } }
        }
    }
}
