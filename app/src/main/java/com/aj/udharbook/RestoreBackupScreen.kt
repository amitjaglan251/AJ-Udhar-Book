package com.aj.udharbook.ui.backup

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aj.udharbook.backup.BackupManager
import kotlinx.coroutines.launch

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun RestoreBackupScreen(
    backupManager: BackupManager,
    onFinished: () -> Unit
) {

    val scope = rememberCoroutineScope()

    val snackbarHostState =
        remember {
            SnackbarHostState()
        }

    var selectedUri by remember {
        mutableStateOf<Uri?>(null)
    }

    var selectedFileName by remember {
        mutableStateOf("")
    }

    var replaceExisting by remember {
        mutableStateOf(false)
    }

    var showConfirmDialog by remember {
        mutableStateOf(false)
    }

    var isRestoring by remember {
        mutableStateOf(false)
    }

    // ==================================================
    // FILE PICKER
    // ==================================================

    val filePicker =
        rememberLauncherForActivityResult(
            contract =
                ActivityResultContracts.OpenDocument()
        ) { uri ->

            if (uri != null) {

                selectedUri = uri

                selectedFileName =
                    uri.lastPathSegment
                        ?.substringAfterLast("/")
                        ?: "Backup File"
            }
        }


    Scaffold(

        topBar = {

            TopAppBar(
                title = {
                    Text("Restore Backup")
                }
            )
        },

        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState
            )
        }

    ) { paddingValues ->

        Column(

            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),

            verticalArrangement =
                Arrangement.spacedBy(16.dp)
        ) {

            // ==================================================
            // INFORMATION
            // ==================================================

            Card(
                modifier =
                    Modifier.fillMaxWidth()
            ) {

                Column(
                    modifier =
                        Modifier.padding(16.dp),

                    verticalArrangement =
                        Arrangement.spacedBy(8.dp)
                ) {

                    Text(
                        text = "Restore Backup",
                        style =
                            MaterialTheme.typography
                                .titleLarge
                    )

                    Text(
                        text =
                            "अपनी AJ Udhar Book की JSON backup file चुनें और data restore करें।"
                    )
                }
            }


            // ==================================================
            // SELECT FILE
            // ==================================================

            Button(

                onClick = {

                    filePicker.launch(
                        arrayOf(
                            "application/json",
                            "text/json",
                            "text/plain",
                            "*/*"
                        )
                    )
                },

                modifier =
                    Modifier.fillMaxWidth()
            ) {

                Text("Select Backup File")
            }


            // ==================================================
            // SELECTED FILE
            // ==================================================

            if (selectedUri != null) {

                Card(
                    modifier =
                        Modifier.fillMaxWidth()
                ) {

                    Column(
                        modifier =
                            Modifier.padding(16.dp),

                        verticalArrangement =
                            Arrangement.spacedBy(8.dp)
                    ) {

                        Text(
                            text = "Selected Backup"
                        )

                        Text(
                            text = selectedFileName,
                            style =
                                MaterialTheme.typography
                                    .bodyMedium
                        )
                    }
                }
            }


            // ==================================================
            // RESTORE MODE
            // ==================================================

            Card(
                modifier =
                    Modifier.fillMaxWidth()
            ) {

                Column(
                    modifier =
                        Modifier.padding(16.dp),

                    verticalArrangement =
                        Arrangement.spacedBy(8.dp)
                ) {

                    Text(
                        text = "Restore Mode",
                        style =
                            MaterialTheme.typography
                                .titleMedium
                    )


                    // ------------------------------------------
                    // ADD TO EXISTING
                    // ------------------------------------------

                    androidx.compose.foundation.layout.Row(
                        modifier =
                            Modifier.fillMaxWidth(),

                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {

                        RadioButton(

                            selected =
                                !replaceExisting,

                            onClick = {
                                replaceExisting = false
                            }
                        )

                        Text(
                            text =
                                "Add to Existing Data"
                        )
                    }


                    // ------------------------------------------
                    // REPLACE EXISTING
                    // ------------------------------------------

                    androidx.compose.foundation.layout.Row(
                        modifier =
                            Modifier.fillMaxWidth(),

                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {

                        RadioButton(

                            selected =
                                replaceExisting,

                            onClick = {
                                replaceExisting = true
                            }
                        )

                        Text(
                            text =
                                "Replace Existing Data"
                        )
                    }
                }
            }


            // ==================================================
            // WARNING
            // ==================================================

            if (replaceExisting) {

                Card(
                    modifier =
                        Modifier.fillMaxWidth()
                ) {

                    Text(
                        text =
                            "⚠️ Replace Existing Data चुनने पर वर्तमान customers और transactions हटाकर backup restore किया जाएगा।",

                        modifier =
                            Modifier.padding(16.dp)
                    )
                }
            }


            // ==================================================
            // RESTORE BUTTON
            // ==================================================

            Button(

                enabled =
                    selectedUri != null &&
                            !isRestoring,

                onClick = {

                    showConfirmDialog = true
                },

                modifier =
                    Modifier.fillMaxWidth()
            ) {

                if (isRestoring) {

                    CircularProgressIndicator(
                        modifier =
                            Modifier.padding(4.dp)
                    )

                } else {

                    Text("Restore Backup")
                }
            }
        }
    }


    // ==================================================
    // CONFIRMATION DIALOG
    // ==================================================

    if (showConfirmDialog) {

        AlertDialog(

            onDismissRequest = {
                showConfirmDialog = false
            },

            title = {
                Text("Restore Backup?")
            },

            text = {

                if (replaceExisting) {

                    Text(
                        "क्या आप existing data को हटाकर backup restore करना चाहते हैं?"
                    )

                } else {

                    Text(
                        "क्या आप backup data को existing data के साथ जोड़ना चाहते हैं?"
                    )
                }
            },

            confirmButton = {

                TextButton(

                    onClick = {

                        showConfirmDialog = false

                        val uri =
                            selectedUri
                                ?: return@TextButton

                        scope.launch {

                            isRestoring = true

                            try {

                                val backupData =
                                    backupManager
                                        .readBackupFromUri(uri)

                                backupManager
                                    .restoreBackup(
                                        backupData =
                                            backupData,

                                        replaceExisting =
                                            replaceExisting
                                    )

                                isRestoring = false

                                snackbarHostState.showSnackbar(
                                    "Backup successfully restored."
                                )

                                onFinished()

                            } catch (e: Exception) {

                                isRestoring = false

                                snackbarHostState.showSnackbar(
                                    "Restore failed: ${
                                        e.message
                                            ?: "Unknown error"
                                    }"
                                )
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
                        showConfirmDialog = false
                    }
                ) {

                    Text("Cancel")
                }
            }
        )
    }
}