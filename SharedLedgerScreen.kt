package com.aj.udharbook.ui.sharedledger

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aj.udharbook.sync.FirestoreSyncManager

@Composable
fun SharedLedgerScreen(
    firestoreSyncManager: FirestoreSyncManager,
    onJoined: () -> Unit,
    onBack: () -> Unit
) {
    var code by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("🔗 Shared Ledger") }) }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Join Customer Ledger", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text("Customer ke SMS me mila 6-digit code yahan enter karein.")

            OutlinedTextField(
                value = code,
                onValueChange = { value ->
                    code = value.filter(Char::isDigit).take(6)
                    error = null
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("6-digit Share Code") },
                singleLine = true
            )

            Button(
                onClick = {
                    loading = true
                    error = null
                    kotlinx.coroutines.MainScope().launch {
                        try {
                            firestoreSyncManager.joinShareCode(code)
                            onJoined()
                        } catch (e: Exception) {
                            error = e.message ?: "Join failed"
                        } finally {
                            loading = false
                        }
                    }
                },
                enabled = code.length == 6 && !loading,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (loading) CircularProgressIndicator(modifier = Modifier.padding(2.dp)) else Text("Join Shared Ledger")
            }

            if (error != null) {
                Text(error ?: "", color = MaterialTheme.colorScheme.error)
            }

            Button(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Back") }
        }
    }
}
