package com.aj.udharbook.navigation

import android.content.Intent
import android.net.Uri
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.aj.udharbook.R
import com.aj.udharbook.backup.BackupManager
import com.aj.udharbook.sync.FirestoreSyncManager
import com.aj.udharbook.ui.auth.LoginScreen
import com.aj.udharbook.ui.backup.BackupRestoreScreen
import com.aj.udharbook.ui.backup.RestoreBackupScreen
import com.aj.udharbook.ui.customer.AddCustomerScreen
import com.aj.udharbook.ui.customer.CustomerDetailsScreen
import com.aj.udharbook.ui.customer.CustomerListScreen
import com.aj.udharbook.ui.customer.EditCustomerScreen
import com.aj.udharbook.ui.dashboard.DashboardScreen
import com.aj.udharbook.ui.payment.PaymentScreen
import com.aj.udharbook.ui.reports.ReportsScreen
import com.aj.udharbook.ui.sharedledger.SharedLedgerScreen
import com.aj.udharbook.ui.transaction.AddTransactionScreen
import com.aj.udharbook.viewmodel.CustomerViewModel
import com.aj.udharbook.viewmodel.TransactionViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import java.util.Locale

@Composable
fun AJNavGraph(
    navController: NavHostController,
    customerViewModel: CustomerViewModel,
    transactionViewModel: TransactionViewModel,
    backupManager: BackupManager,
    firestoreSyncManager: FirestoreSyncManager
) {
    NavHost(navController = navController, startDestination = Screen.Login.route) {
        composable(Screen.Login.route) {
            val scope = rememberCoroutineScope()
            LoginScreen(onLoginSuccess = {
                scope.launch {
                    try {
                        firestoreSyncManager.restoreCloudToLocal()
                        firestoreSyncManager.syncAllSharedLedgers()
                    } catch (e: Exception) { e.printStackTrace() }
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            })
        }

        composable(Screen.Dashboard.route) {
            val customers by customerViewModel.allCustomers.collectAsState(initial = emptyList())
            val transactions by transactionViewModel.allTransactions.collectAsState(initial = emptyList())
            val scope = rememberCoroutineScope()

            LaunchedEffect(customers) {
                try { firestoreSyncManager.syncAllSharedLedgers() } catch (_: Exception) { }
            }

            DashboardScreen(
                customers = customers,
                transactions = transactions,
                joinRequestCount = 0,
                onAddCustomer = { navController.navigate(Screen.AddCustomer.route) },
                onViewCustomers = { navController.navigate(Screen.CustomerList.route) },
                onViewReports = { navController.navigate(Screen.Reports.route) },
                onBackupRestore = { navController.navigate(Screen.Backup.route) },
                onSharedLedger = { navController.navigate(Screen.SharedLedger.route) },
                onJoinRequests = { navController.navigate(Screen.SharedLedger.route) },
                onSignOut = {
                    scope.launch {
                        try { firestoreSyncManager.clearLocalData() } catch (e: Exception) { e.printStackTrace() }
                        FirebaseAuth.getInstance().signOut()
                        val options = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                            .requestIdToken(navController.context.getString(R.string.default_web_client_id))
                            .requestEmail()
                            .build()
                        GoogleSignIn.getClient(navController.context, options).signOut().addOnCompleteListener {
                            navController.navigate(Screen.Login.route) {
                                popUpTo(Screen.Dashboard.route) { inclusive = true }
                                launchSingleTop = true
                            }
                        }
                    }
                }
            )
        }

        composable(Screen.SharedLedger.route) {
            val customers by customerViewModel.allCustomers.collectAsState(initial = emptyList())
            SharedLedgerScreen(
                customers = customers,
                firestoreSyncManager = firestoreSyncManager,
                onJoined = { navController.popBackStack() },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Reports.route) {
            val customers by customerViewModel.allCustomers.collectAsState(initial = emptyList())
            val transactions by transactionViewModel.allTransactions.collectAsState(initial = emptyList())
            ReportsScreen(
                customers = customers,
                transactions = transactions,
                onCustomerClick = { id -> navController.navigate(Screen.CustomerDetails.createRoute(id)) }
            )
        }

        composable(Screen.Backup.route) {
            BackupRestoreScreen(backupManager = backupManager, onRestoreBackup = { navController.navigate("restore_backup") })
        }
        composable("restore_backup") {
            RestoreBackupScreen(backupManager = backupManager, onFinished = { navController.popBackStack() })
        }
        composable(Screen.AddCustomer.route) {
            AddCustomerScreen(viewModel = customerViewModel, onSaved = { navController.popBackStack() })
        }
        composable(Screen.CustomerList.route) {
            CustomerListScreen(navController = navController, viewModel = customerViewModel)
        }

        composable(
            Screen.CustomerDetails.route,
            arguments = listOf(navArgument("customerId") { type = NavType.IntType })
        ) { entry ->
            val id = entry.arguments?.getInt("customerId") ?: 0
            val customer by customerViewModel.getCustomerById(id).collectAsState(initial = null)
            val transactions by transactionViewModel.getTransactionsByCustomer(id).collectAsState(initial = emptyList())
            if (customer != null) {
                CustomerDetailsScreen(
                    customerName = customer!!.name,
                    mobile = customer!!.mobile,
                    address = customer!!.address,
                    transactions = transactions,
                    sharedLedgerId = customer!!.sharedLedgerId,
                    onAddUdhar = { navController.navigate(Screen.AddTransaction.createRoute(id, "UDHAR")) },
                    onAddPayment = { navController.navigate(Screen.AddTransaction.createRoute(id, "PAYMENT")) },
                    onEditCustomer = { navController.navigate(Screen.EditCustomer.createRoute(id)) },
                    onDeleteCustomer = { customerViewModel.delete(customer!!); navController.popBackStack() },
                    onEditTransaction = { transactionViewModel.update(it) },
                    onDeleteTransaction = { transactionViewModel.delete(it) },
                    onSmsCustomer = {
                        val currentBalance = transactions.filter { it.type.equals("UDHAR", true) }.sumOf { it.amount } -
                            transactions.filter { it.type.equals("PAYMENT", true) }.sumOf { it.amount }
                        scopeLaunchSms(navController, firestoreSyncManager, customer!!, currentBalance)
                    }
                )
            } else {
                CircularProgressIndicator()
            }
        }

        composable(
            Screen.EditCustomer.route,
            arguments = listOf(navArgument("customerId") { type = NavType.IntType })
        ) { entry ->
            val id = entry.arguments?.getInt("customerId") ?: 0
            val customer by customerViewModel.getCustomerById(id).collectAsState(initial = null)
            if (customer != null) {
                EditCustomerScreen(customer = customer!!, viewModel = customerViewModel, onSaved = { navController.popBackStack() })
            } else {
                CircularProgressIndicator()
            }
        }

        composable(
            Screen.AddTransaction.route,
            arguments = listOf(
                navArgument("customerId") { type = NavType.IntType },
                navArgument("type") { type = NavType.StringType }
            )
        ) { entry ->
            val id = entry.arguments?.getInt("customerId") ?: 0
            val type = entry.arguments?.getString("type") ?: "UDHAR"
            val customer by customerViewModel.getCustomerById(id).collectAsState(initial = null)
            val transactions by transactionViewModel.getTransactionsByCustomer(id).collectAsState(initial = emptyList())
            val name = customer?.name ?: ""
            val mobile = customer?.mobile ?: ""
            if (type.equals("PAYMENT", ignoreCase = true)) {
                val balance = transactions.fold(0.0) { total, t ->
                    when {
                        t.type.equals("UDHAR", true) -> total + t.amount
                        t.type.equals("PAYMENT", true) -> total - t.amount
                        else -> total
                    }
                }.coerceAtLeast(0.0)
                PaymentScreen(
                    customerId = id,
                    customerName = name,
                    customerMobile = mobile,
                    currentBalance = balance,
                    transactionViewModel = transactionViewModel,
                    onSaved = { navController.popBackStack() }
                )
            } else {
                AddTransactionScreen(
                    customerId = id,
                    customerName = name,
                    customerMobile = mobile,
                    initialType = type,
                    viewModel = transactionViewModel,
                    onSaved = { navController.popBackStack() }
                )
            }
        }
    }
}

private fun scopeLaunchSms(
    navController: NavHostController,
    syncManager: FirestoreSyncManager,
    customer: com.aj.udharbook.model.Customer,
    balance: Double
) {
    kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
        try {
            val invite = syncManager.createSharedLedger(customer)
            val message = "AJ Udhar Book: ${customer.name} ka shared ledger join karne ke liye app me Secure Invite Token paste karein:\n${invite.token}\nCurrent balance ₹${String.format(Locale.US, "%.2f", balance)}"
            val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:${customer.mobile.trim()}"))
                .putExtra("sms_body", message)
            navController.context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
