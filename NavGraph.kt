package com.aj.udharbook.navigation

import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import com.aj.udharbook.ui.transaction.AddTransactionScreen

import com.aj.udharbook.viewmodel.CustomerViewModel
import com.aj.udharbook.viewmodel.TransactionViewModel

import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth

import kotlinx.coroutines.launch

@Composable
fun AJNavGraph(
    navController: NavHostController,
    customerViewModel: CustomerViewModel,
    transactionViewModel: TransactionViewModel,
    backupManager: BackupManager,
    firestoreSyncManager: FirestoreSyncManager
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Login.route
    ) {
        composable(Screen.Login.route) {
            val scope = rememberCoroutineScope()
            LoginScreen(
                onLoginSuccess = {
                    scope.launch {
                        try {
                            firestoreSyncManager.restoreCloudToLocal()
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                        navController.navigate(Screen.Dashboard.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                }
            )
        }

        composable(Screen.Dashboard.route) {
            val customers by customerViewModel.allCustomers.collectAsState(initial = emptyList())
            val transactions by transactionViewModel.allTransactions.collectAsState(initial = emptyList())
            val scope = rememberCoroutineScope()

            DashboardScreen(
                customers = customers,
                transactions = transactions,
                onAddCustomer = { navController.navigate(Screen.AddCustomer.route) },
                onViewCustomers = { navController.navigate(Screen.CustomerList.route) },
                onViewReports = { navController.navigate(Screen.Reports.route) },
                onBackupRestore = { navController.navigate(Screen.Backup.route) },
                onSignOut = {
                    scope.launch {
                        // IMPORTANT: Do NOT clear Room data on logout.
                        // Local customer/transaction data must remain available,
                        // while Firestore keeps the cloud backup for reinstall/restore.
                        try {
                            FirebaseAuth.getInstance().signOut()

                            val googleSignInOptions = GoogleSignInOptions.Builder(
                                GoogleSignInOptions.DEFAULT_SIGN_IN
                            )
                                .requestIdToken(
                                    navController.context.getString(
                                        R.string.default_web_client_id
                                    )
                                )
                                .requestEmail()
                                .build()

                            val googleSignInClient = GoogleSignIn.getClient(
                                navController.context,
                                googleSignInOptions
                            )

                            googleSignInClient.signOut().addOnCompleteListener {
                                navController.navigate(Screen.Login.route) {
                                    popUpTo(0) { inclusive = true }
                                    launchSingleTop = true
                                }
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                            FirebaseAuth.getInstance().signOut()
                            navController.navigate(Screen.Login.route) {
                                popUpTo(0) { inclusive = true }
                                launchSingleTop = true
                            }
                        }
                    }
                }
            )
        }

        composable(Screen.Reports.route) {
            val customers by customerViewModel.allCustomers.collectAsState(initial = emptyList())
            val transactions by transactionViewModel.allTransactions.collectAsState(initial = emptyList())
            ReportsScreen(
                customers = customers,
                transactions = transactions,
                onCustomerClick = { customerId ->
                    navController.navigate(Screen.CustomerDetails.createRoute(customerId))
                }
            )
        }

        composable(Screen.Backup.route) {
            BackupRestoreScreen(
                backupManager = backupManager,
                onRestoreBackup = { navController.navigate("restore_backup") }
            )
        }

        composable("restore_backup") {
            RestoreBackupScreen(
                backupManager = backupManager,
                onFinished = { navController.popBackStack() }
            )
        }

        composable(Screen.AddCustomer.route) {
            AddCustomerScreen(
                viewModel = customerViewModel,
                onSaved = { navController.popBackStack() }
            )
        }

        composable(Screen.CustomerList.route) {
            CustomerListScreen(
                navController = navController,
                viewModel = customerViewModel
            )
        }

        composable(
            route = Screen.CustomerDetails.route,
            arguments = listOf(
                navArgument("customerId") { type = NavType.IntType }
            )
        ) { backStackEntry ->
            val customerId = backStackEntry.arguments?.getInt("customerId") ?: 0
            val customer by customerViewModel.getCustomerById(customerId).collectAsState(initial = null)
            val transactions by transactionViewModel.getTransactionsByCustomer(customerId)
                .collectAsState(initial = emptyList())

            if (customer != null) {
                CustomerDetailsScreen(
                    customerName = customer!!.name,
                    mobile = customer!!.mobile,
                    address = customer!!.address,
                    transactions = transactions,
                    onAddUdhar = {
                        navController.navigate(
                            Screen.AddTransaction.createRoute(customerId, "UDHAR")
                        )
                    },
                    onAddPayment = {
                        navController.navigate(
                            Screen.AddTransaction.createRoute(customerId, "PAYMENT")
                        )
                    },
                    onEditCustomer = {
                        navController.navigate(Screen.EditCustomer.createRoute(customerId))
                    },
                    onDeleteCustomer = {
                        customerViewModel.delete(customer!!)
                        navController.popBackStack()
                    },
                    onEditTransaction = { transactionViewModel.update(it) },
                    onDeleteTransaction = { transactionViewModel.delete(it) }
                )
            } else {
                CircularProgressIndicator()
            }
        }

        composable(
            route = Screen.EditCustomer.route,
            arguments = listOf(
                navArgument("customerId") { type = NavType.IntType }
            )
        ) { backStackEntry ->
            val customerId = backStackEntry.arguments?.getInt("customerId") ?: 0
            val customer by customerViewModel.getCustomerById(customerId).collectAsState(initial = null)
            if (customer != null) {
                EditCustomerScreen(
                    customer = customer!!,
                    viewModel = customerViewModel,
                    onSaved = { navController.popBackStack() }
                )
            } else {
                CircularProgressIndicator()
            }
        }

        composable(
            route = Screen.AddTransaction.route,
            arguments = listOf(
                navArgument("customerId") { type = NavType.IntType },
                navArgument("type") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val customerId = backStackEntry.arguments?.getInt("customerId") ?: 0
            val type = backStackEntry.arguments?.getString("type") ?: "UDHAR"
            val customer by customerViewModel.getCustomerById(customerId).collectAsState(initial = null)
            val transactions by transactionViewModel.getTransactionsByCustomer(customerId)
                .collectAsState(initial = emptyList())
            val customerName = customer?.name ?: ""
            val customerMobile = customer?.mobile ?: ""

            if (type.equals("PAYMENT", ignoreCase = true)) {
                val currentBalance = transactions.fold(0.0) { balance, transaction ->
                    when {
                        transaction.type.equals("UDHAR", ignoreCase = true) -> balance + transaction.amount
                        transaction.type.equals("PAYMENT", ignoreCase = true) -> balance - transaction.amount
                        else -> balance
                    }
                }.coerceAtLeast(0.0)

                PaymentScreen(
                    customerName = customerName,
                    customerMobile = customerMobile,
                    currentBalance = currentBalance,
                    onPaymentAdded = { amount, note ->
                        transactionViewModel.insert(
                            com.aj.udharbook.model.Transaction(
                                customerId = customerId,
                                amount = amount,
                                type = "PAYMENT",
                                note = note,
                                timestamp = System.currentTimeMillis()
                            )
                        ) { navController.popBackStack() }
                    },
                    onBack = { navController.popBackStack() }
                )
            } else {
                AddTransactionScreen(
                    customerId = customerId,
                    customerName = customerName,
                    customerMobile = customerMobile,
                    viewModel = transactionViewModel,
                    onSaved = { navController.popBackStack() }
                )
            }
        }
    }
}
