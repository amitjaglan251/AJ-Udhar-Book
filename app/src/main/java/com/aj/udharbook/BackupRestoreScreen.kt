package com.aj.udharbook.ui.backup

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aj.udharbook.backup.BackupManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupRestoreScreen(
    backupManager: BackupManager,
    onRestoreBackup: () -> Unit,
    onSyncNow: suspend () -> Unit
) {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var isCreatingBackup by remember { mutableStateOf(false) }
    var isSyncing by remember { mutableStateOf(false) }

    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                isCreatingBackup = true
                try {
                    val json = backupManager.createBackup()
                    backupManager.saveJsonToUri(uri, json)
                    snackbarHostState.showSnackbar("Backup successfully saved.")
                } catch (e: Exception) {
                    snackbarHostState.showSnackbar(
                        "Backup failed: ${e.message ?: "Unknown error"}"
                    )
                } finally {
                    isCreatingBackup = false
                }
            }
        }
    }

    Scaffold(
        topBar = {
            androidx.compose.material3.TopAppBar(
                title = { Text("Backup & Restore") }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "AJ Udhar Book Backup",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Text(
                        text = "अपने customers और transactions का backup सुरक्षित रखें।"
                    )
                }
            }

            Button(
                modifier = Modifier.fillMaxWidth(),
                enabled = !isCreatingBackup && !isSyncing,
                onClick = {
                    createDocumentLauncher.launch("AJ_UdharBook_Backup.json")
                }
            ) {
                if (isCreatingBackup) {
                    CircularProgressIndicator(modifier = Modifier.height(20.dp))
                } else {
                    Text("Create Backup")
                }
            }

            Button(
                modifier = Modifier.fillMaxWidth(),
                enabled = !isCreatingBackup && !isSyncing,
                onClick = onRestoreBackup
            ) {
                Text("Restore Backup")
            }

            Button(
                modifier = Modifier.fillMaxWidth(),
                enabled = !isCreatingBackup && !isSyncing,
                onClick = {
                    scope.launch {
                        isSyncing = true
                        try {
                            onSyncNow()
                            snackbarHostState.showSnackbar(
                                "Cloud sync completed successfully."
                            )
                        } catch (e: Exception) {
                            snackbarHostState.showSnackbar(
                                "Sync failed: ${e.message ?: "Internet or Firebase error"}"
                            )
                        } finally {
                            isSyncing = false
                        }
                    }
                }
            ) {
                if (isSyncing) {
                    CircularProgressIndicator(modifier = Modifier.height(20.dp))
                } else {
                    Text("Sync Now")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Cloud Sync",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "Room का local data सुरक्षित रहेगा और Sync Now से current customers तथा Udhar/Payment transactions Firestore में दोबारा भेजे जा सकते हैं।"
                    )
                    Text(
                        text = "Internet unavailable होने पर local data delete नहीं होगा। बाद में Sync Now से retry किया जा सकता है।"
                    )
                }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Backup Information",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "Backup में customers और उनके सभी Udhar/Payment transactions सुरक्षित किए जाएंगे।"
                    )
                    Text(text = "Backup file JSON format में save होगी।")
                    Text(
                        text = "Restore करते समय Add या Replace Existing Data का विकल्प मिलेगा।"
                    )
                }
            }
        }
    }
}
