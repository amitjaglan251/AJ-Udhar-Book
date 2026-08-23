package com.aj.udharbook.ui.backup

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.aj.udharbook.backup.BackupManager
import com.aj.udharbook.database.AppDatabase
import com.aj.udharbook.model.Customer
import com.aj.udharbook.sync.FirestoreSyncManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupRestoreScreen(
    backupManager: BackupManager,
    onRestoreBackup: () -> Unit
) {

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val snackbarHostState = remember {
        SnackbarHostState()
    }

    val firestoreSyncManager = remember(context) {
        val database = AppDatabase.getDatabase(context)
        FirestoreSyncManager(
            customerDao = database.customerDao(),
            transactionDao = database.transactionDao()
        )
    }

    var isCreatingBackup by remember {
        mutableStateOf(false)
    }

    var customers by remember {
        mutableStateOf<List<Customer>>(emptyList())
    }

    var connectionCode by remember {
        mutableStateOf("")
    }

    var generatedCode by remember {
        mutableStateOf<String?>(null)
    }

    var isConnectionWorking by remember {
        mutableStateOf(false)
    }

    // ==================================================
    // LOAD LOCAL CUSTOMERS
    // ==================================================

    LaunchedEffect(Unit) {
        try {
            customers = firestoreSyncManager.getLocalCustomers()
        } catch (e: Exception) {
            snackbarHostState.showSnackbar(
                "Customers load नहीं हुए: ${e.message ?: "Unknown error"}"
            )
        }
    }

    // ==================================================
    // NOTIFICATION PERMISSION
    // ==================================================

    val notificationPermissionLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->
            scope.launch {
                snackbarHostState.showSnackbar(
                    if (granted) {
                        "App notifications ON हो गई हैं।"
                    } else {
                        "Notification permission allow नहीं हुई।"
                    }
                )
            }
        }

    // ==================================================
    // CREATE FILE PICKER
    // ==================================================

    val createDocumentLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.CreateDocument(
                "application/json"
            )
        ) { uri ->

            if (uri != null) {
                scope.launch {
                    isCreatingBackup = true

                    try {
                        val json = backupManager.createBackup()

                        backupManager.saveJsonToUri(
                            uri = uri,
                            jsonText = json
                        )

                        snackbarHostState.showSnackbar(
                            "Backup successfully saved."
                        )

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

    // ==================================================
    // UI
    // ==================================================

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Backup & Cloud Sync")
                }
            )
        },
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState
            )
        }
    ) { paddingValues ->

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // ==================================================
            // APP-TO-APP SYNC HEADER
            // ==================================================

            item {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "🔗 App-to-App Cloud Sync",
                            style = MaterialTheme.typography.titleLarge
                        )

                        Text(
                            text =
                                "Customer को इस app से जोड़ें। इसके बाद Udhar/Payment update उसके AJ Udhar Book में cloud से sync होगा और app notification आएगा।"
                        )

                        Button(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    if (
                                        ContextCompat.checkSelfPermission(
                                            context,
                                            Manifest.permission.POST_NOTIFICATIONS
                                        ) != PackageManager.PERMISSION_GRANTED
                                    ) {
                                        notificationPermissionLauncher.launch(
                                            Manifest.permission.POST_NOTIFICATIONS
                                        )
                                    } else {
                                        scope.launch {
                                            snackbarHostState.showSnackbar(
                                                "Notifications पहले से ON हैं।"
                                            )
                                        }
                                    }
                                } else {
                                    scope.launch {
                                        snackbarHostState.showSnackbar(
                                            "इस Android version पर अलग notification permission की जरूरत नहीं है।"
                                        )
                                    }
                                }
                            }
                        ) {
                            Text("Enable App Notifications")
                        }
                    }
                }
            }

            // ==================================================
            // CREATE LINK CODE
            // ==================================================

            item {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Customer को Connect करें",
                            style = MaterialTheme.typography.titleMedium
                        )

                        Text(
                            text =
                                "जिस customer के साथ हिसाब share करना है, उसके सामने Connect दबाएँ।"
                        )

                        if (customers.isEmpty()) {
                            Text("पहले कम से कम एक customer add करें।")
                        } else {
                            customers.forEach { customer ->
                                Button(
                                    modifier = Modifier.fillMaxWidth(),
                                    onClick = {
                                        scope.launch {
                                            isConnectionWorking = true

                                            try {
                                                generatedCode =
                                                    firestoreSyncManager.createConnection(
                                                        customer.id
                                                    )

                                                snackbarHostState.showSnackbar(
                                                    "Connection code तैयार हो गया।"
                                                )
                                            } catch (e: Exception) {
                                                snackbarHostState.showSnackbar(
                                                    "Connection failed: ${e.message ?: "Unknown error"}"
                                                )
                                            } finally {
                                                isConnectionWorking = false
                                            }
                                        }
                                    },
                                    enabled = !isConnectionWorking
                                ) {
                                    Text(
                                        "Connect: ${customer.name} (${customer.mobile})"
                                    )
                                }
                            }
                        }

                        generatedCode?.let { code ->
                            Card(
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text("Customer को यह code दें:")
                                    Text(
                                        text = code,
                                        style = MaterialTheme.typography.headlineMedium
                                    )
                                    Text(
                                        "दूसरे phone में AJ Udhar Book → Backup & Cloud Sync खोलकर यह code डालें।"
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ==================================================
            // ACCEPT LINK CODE
            // ==================================================

            item {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Customer के रूप में Connect करें",
                            style = MaterialTheme.typography.titleMedium
                        )

                        Text(
                            text =
                                "दूसरे व्यक्ति से मिला 8-digit connection code यहाँ डालें।"
                        )

                        OutlinedTextField(
                            value = connectionCode,
                            onValueChange = {
                                connectionCode =
                                    it.uppercase().filter { char ->
                                        char.isLetterOrDigit()
                                    }.take(8)
                            },
                            label = {
                                Text("Connection Code")
                            },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Button(
                            modifier = Modifier.fillMaxWidth(),
                            enabled =
                                connectionCode.length == 8 &&
                                        !isConnectionWorking,
                            onClick = {
                                scope.launch {
                                    isConnectionWorking = true

                                    try {
                                        val customerName =
                                            firestoreSyncManager.acceptConnection(
                                                connectionCode
                                            )

                                        snackbarHostState.showSnackbar(
                                            "$customerName से connection successfully active हो गया।"
                                        )
                                    } catch (e: Exception) {
                                        snackbarHostState.showSnackbar(
                                            "Connection accept failed: ${e.message ?: "Unknown error"}"
                                        )
                                    } finally {
                                        isConnectionWorking = false
                                    }
                                }
                            }
                        ) {
                            Text("Accept & Connect")
                        }
                    }
                }
            }

            // ==================================================
            // BACKUP HEADER
            // ==================================================

            item {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "AJ Udhar Book Backup",
                            style = MaterialTheme.typography.headlineSmall
                        )

                        Text(
                            text =
                                "अपने customers और transactions का backup सुरक्षित रखें।"
                        )
                    }
                }
            }

            // ==================================================
            // CREATE BACKUP
            // ==================================================

            item {
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isCreatingBackup,
                    onClick = {
                        createDocumentLauncher.launch(
                            "AJ_UdharBook_Backup.json"
                        )
                    }
                ) {
                    if (isCreatingBackup) {
                        CircularProgressIndicator(
                            modifier = Modifier.height(20.dp)
                        )
                    } else {
                        Text("Create Backup")
                    }
                }
            }

            // ==================================================
            // RESTORE BACKUP
            // ==================================================

            item {
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isCreatingBackup,
                    onClick = onRestoreBackup
                ) {
                    Text("Restore Backup")
                }
            }

            // ==================================================
            // INFORMATION
            // ==================================================

            item {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Cloud Sync Information",
                            style = MaterialTheme.typography.titleMedium
                        )

                        Text(
                            text =
                                "Linked customer को सिर्फ उसका linked हिसाब मिलेगा। बाकी customers का data share नहीं किया जाएगा।"
                        )

                        Text(
                            text =
                                "Internet उपलब्ध होने पर Udhar/Payment update Firestore के जरिए दूसरे app तक पहुँचेगा।"
                        )

                        Text(
                            text =
                                "Backup file JSON format में save होगी और manual restore अलग से काम करता रहेगा।"
                        )
                    }
                }
            }
        }
    }
}
