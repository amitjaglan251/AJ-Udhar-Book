package com.aj.udharbook.navigation

sealed class Screen(val route: String) {

    // ==================================================
    // AUTH
    // ==================================================

    object Login : Screen("login")


    // ==================================================
    // DASHBOARD
    // ==================================================

    object Dashboard : Screen("dashboard")

    object Reports : Screen("reports")


    // ==================================================
    // CUSTOMER
    // ==================================================

    object AddCustomer : Screen("add_customer")

    object CustomerList : Screen("customers")

    object Backup : Screen("backup")


    // ==================================================
    // CUSTOMER DETAILS
    // ==================================================

    object CustomerDetails :
        Screen("customer_details/{customerId}") {

        fun createRoute(
            customerId: Int
        ): String {
            return "customer_details/$customerId"
        }
    }


    // ==================================================
    // EDIT CUSTOMER
    // ==================================================

    object EditCustomer :
        Screen("edit_customer/{customerId}") {

        fun createRoute(
            customerId: Int
        ): String {
            return "edit_customer/$customerId"
        }
    }


    // ==================================================
    // ADD TRANSACTION
    // ==================================================

    object AddTransaction :
        Screen("add_transaction/{customerId}/{type}") {

        fun createRoute(
            customerId: Int,
            type: String
        ): String {
            return "add_transaction/$customerId/$type"
        }
    }
}