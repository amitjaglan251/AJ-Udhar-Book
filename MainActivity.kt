package com.aj.udharbook

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.navigation.compose.rememberNavController
import com.aj.udharbook.backup.BackupManager
import com.aj.udharbook.database.AppDatabase
import com.aj.udharbook.navigation.AJNavGraph
import com.aj.udharbook.repository.CustomerRepository
import com.aj.udharbook.repository.TransactionRepository
import com.aj.udharbook.sync.CloudSyncScheduler
import com.aj.udharbook.sync.FirestoreSyncManager
import com.aj.udharbook.viewmodel.CustomerViewModel
import com.aj.udharbook.viewmodel.CustomerViewModelFactory
import com.aj.udharbook.viewmodel.TransactionViewModel
import com.aj.udharbook.viewmodel.TransactionViewModelFactory

class MainActivity : ComponentActivity() {

    private val database by lazy {
        AppDatabase.getDatabase(applicationContext)
    }

    private val customerRepository by lazy {
        CustomerRepository(database.customerDao())
    }

    private val transactionRepository by lazy {
        TransactionRepository(database.transactionDao())
    }

    private val firestoreSyncManager by lazy {
        FirestoreSyncManager(
            customerDao = database.customerDao(),
            transactionDao = database.transactionDao()
        )
    }

    private val customerViewModel: CustomerViewModel by viewModels {
        CustomerViewModelFactory(
            customerRepository,
            firestoreSyncManager
        )
    }

    private val transactionViewModel: TransactionViewModel by viewModels {
        TransactionViewModelFactory(
            transactionRepository,
            firestoreSyncManager
        )
    }

    private val backupManager by lazy {
        BackupManager(
            context = applicationContext,
            database = database
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Keep a single background sync job registered. WorkManager will run it
        // whenever network connectivity is available, even after app restarts.
        CloudSyncScheduler.schedule(applicationContext)

        setContent {
            val navController = rememberNavController()

            AJNavGraph(
                navController = navController,
                customerViewModel = customerViewModel,
                transactionViewModel = transactionViewModel,
                backupManager = backupManager,
                firestoreSyncManager = firestoreSyncManager
            )
        }
    }
}
