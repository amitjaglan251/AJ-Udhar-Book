package com.aj.udharbook.navigation

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Dashboard : Screen("dashboard")
    object Reports : Screen("reports")
    object AddCustomer : Screen("add_customer")
    object CustomerList : Screen("customers")
    object Backup : Screen("backup")
    object SharedLedger : Screen("shared_ledger")

    object CustomerDetails : Screen("customer_details/{customerId}") {
        fun createRoute(customerId: Int): String = "customer_details/$customerId"
    }

    object EditCustomer : Screen("edit_customer/{customerId}") {
        fun createRoute(customerId: Int): String = "edit_customer/$customerId"
    }

    object AddTransaction : Screen("add_transaction/{customerId}/{type}") {
        fun createRoute(customerId: Int, type: String): String = "add_transaction/$customerId/$type"
    }
}
