package com.aj.udharbook.ui.backup

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aj.udharbook.backup.BackupData
import com.aj.udharbook.backup.BackupManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupScreen(
    backupManager: BackupManager
) {

    val scope = rememberCoroutineScope()

    var isLoading by remember {
        mutableStateOf(false)
    }

    var backupJson by remember {
        mutableStateOf<String?>(null)
    }

    var selectedBackup by remember {
        mutableStateOf<BackupData?>(null)
    }

    var showRestoreDialog by remember {
        mutableStateOf(false)
    }

    var message by remember {
        mutableStateOf("")
    }


    // =====================================================
    // SAVE BACKUP FILE PICKER
    // =====================================================

    val saveFileLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.CreateDocument(
                "application/json"
            )
        ) { uri ->

            if (uri != null && backupJson != null) {

                scope.launch {

                    try {

                        backupManager.saveJsonToUri(
                            uri = uri,
                            jsonText = backupJson!!
                        )

                        message =
                            "Backup सफलतापूर्वक Save हो गया।"

                    } catch (e: Exception) {

                        message =
                            "Backup Save failed: ${e.message}"
                    }
                }

            } else {

                message =
                    "Backup Save cancel किया गया।"
            }
        }


    // =====================================================
    // RESTORE FILE PICKER
    // =====================================================

    val restoreFileLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.OpenDocument()
        ) { uri ->

            if (uri != null) {

                scope.launch {

                    try {

                        isLoading = true

                        val backupData =
                            backupManager.readBackupFromUri(uri)

                        selectedBackup = backupData

                        showRestoreDialog = true

                    } catch (e: Exception) {

                        message =
                            "Backup file invalid है: ${e.message}"

                    } finally {

                        isLoading = false
                    }
                }

            } else {

                message =
                    "Restore cancel किया गया।"
            }
        }


    // =====================================================
    // RESTORE CONFIRMATION DIALOG
    // =====================================================

    if (showRestoreDialog &&
        selectedBackup != null
    ) {

        AlertDialog(

            onDismissRequest = {
                showRestoreDialog = false
                selectedBackup = null
            },

            title = {
                Text("Restore Backup?")
            },

            text = {

                Column(
                    verticalArrangement =
                        Arrangement.spacedBy(8.dp)
                ) {

                    Text(
                        "Backup में:"
                    )

                    Text(
                        "Customers: " +
                                selectedBackup!!
                                    .customers.size
                    )

                    Text(
                        "Transactions: " +
                                selectedBackup!!
                                    .transactions.size
                    )

                    Text(
                        "क्या आप इस backup को restore करना चाहते हैं?"
                    )

                    Text(
                        "Replace करने पर वर्तमान data हट जाएगा।",
                        style =
                            MaterialTheme.typography.bodySmall
                    )
                }
            },

            confirmButton = {

                TextButton(

                    onClick = {

                        val data =
                            selectedBackup

                        if (data != null) {

                            showRestoreDialog = false
                            isLoading = true

                            scope.launch {

                                try {

                                    backupManager.restoreBackup(
                                        backupData = data,
                                        replaceExisting = true
                                    )

                                    message =
                                        "Restore सफलतापूर्वक पूरा हो गया।"

                                } catch (e: Exception) {

                                    message =
                                        "Restore failed: ${e.message}"

                                } finally {

                                    isLoading = false
                                    selectedBackup = null
                                }
                            }
                        }
                    }

                ) {

                    Text("Restore")
                }
            },

            dismissButton = {

                TextButton(

                    onClick = {

                        showRestoreDialog = false
                        selectedBackup = null
                    }

                ) {

                    Text("Cancel")
                }
            }
        )
    }


    // =====================================================
    // SCREEN
    // =====================================================

    Scaffold(

        topBar = {

            TopAppBar(
                title = {
                    Text("Backup & Restore")
                }
            )
        }

    ) { paddingValues ->

        Column(

            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(20.dp),

            verticalArrangement =
                Arrangement.spacedBy(16.dp)
        ) {

            Text(
                text = "AJ Udhar Book Backup",
                style =
                    MaterialTheme.typography.headlineSmall
            )

            Text(
                text =
                    "अपने Customers और Transactions का backup सुरक्षित रखें।"
            )


            // =================================================
            // CREATE BACKUP
            // =================================================

            Button(

                modifier = Modifier.fillMaxWidth(),

                enabled = !isLoading,

                onClick = {

                    scope.launch {

                        isLoading = true
                        message = ""

                        try {

                            backupJson =
                                backupManager.createBackup()

                            saveFileLauncher.launch(
                                "AJ_UdharBook_Backup.json"
                            )

                        } catch (e: Exception) {

                            message =
                                "Backup failed: ${e.message}"

                        } finally {

                            isLoading = false
                        }
                    }
                }

            ) {

                if (isLoading) {

                    CircularProgressIndicator()

                } else {

                    Text("Create Backup")
                }
            }


            // =================================================
            // RESTORE BACKUP
            // =================================================

            Button(

                modifier = Modifier.fillMaxWidth(),

                enabled = !isLoading,

                onClick = {

                    restoreFileLauncher.launch(
                        arrayOf("application/json")
                    )
                }

            ) {

                Text("Restore Backup")
            }


            // =================================================
            // MESSAGE
            // =================================================

            if (message.isNotBlank()) {

                Text(
                    text = message,
                    style =
                        MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
}