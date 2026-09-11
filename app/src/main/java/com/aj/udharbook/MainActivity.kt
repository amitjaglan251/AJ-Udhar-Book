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
import com.aj.udharbook.sync.FirestoreSyncManager
import com.aj.udharbook.viewmodel.CustomerViewModel
import com.aj.udharbook.viewmodel.CustomerViewModelFactory
import com.aj.udharbook.viewmodel.TransactionViewModel
import com.aj.udharbook.viewmodel.TransactionViewModelFactory

class MainActivity : ComponentActivity() {

    // ==================================================
    // DATABASE
    // ==================================================

    private val database by lazy {
        AppDatabase.getDatabase(applicationContext)
    }

    // ==================================================
    // REPOSITORIES
    // ==================================================

    private val customerRepository by lazy {
        CustomerRepository(
            database.customerDao()
        )
    }

    private val transactionRepository by lazy {
        TransactionRepository(
            database.transactionDao()
        )
    }

    // ==================================================
    // FIRESTORE SYNC MANAGER
    // ==================================================

    private val firestoreSyncManager by lazy {
        FirestoreSyncManager(
            customerDao = database.customerDao(),
            transactionDao = database.transactionDao()
        )
    }

    // ==================================================
    // CUSTOMER VIEWMODEL
    // ==================================================

    private val customerViewModel: CustomerViewModel by viewModels {

        CustomerViewModelFactory(
            customerRepository,
            firestoreSyncManager
        )
    }

    // ==================================================
    // TRANSACTION VIEWMODEL
    // ==================================================

    private val transactionViewModel: TransactionViewModel by viewModels {

        TransactionViewModelFactory(
            transactionRepository,
            firestoreSyncManager
        )
    }

    // ==================================================
    // BACKUP MANAGER
    // ==================================================

    private val backupManager by lazy {

        BackupManager(
            context = applicationContext,
            database = database
        )
    }

    // ==================================================
    // ON CREATE
    // ==================================================

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {

        super.onCreate(savedInstanceState)

        setContent {

            val navController =
                rememberNavController()

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