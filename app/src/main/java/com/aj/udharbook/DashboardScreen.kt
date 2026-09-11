package com.aj.udharbook.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import com.aj.udharbook.model.Customer
import com.aj.udharbook.model.Transaction
import java.util.Locale

// ==========================================================
// DASHBOARD COLORS
// ==========================================================

private val UdharGreen =
    Color(0xFF2E7D32)

private val PaymentRed =
    Color(0xFFD32F2F)

private val CustomerBlue =
    Color(0xFF1976D2)

private val TransactionPurple =
    Color(0xFF7B1FA2)

private val BalanceOrange =
    Color(0xFFEF6C00)

private val ReportsBlue =
    Color(0xFF1565C0)

private val BackupGreen =
    Color(0xFF388E3C)

private val SignOutRed =
    Color(0xFFC62828)


// ==========================================================
// DASHBOARD SCREEN
// ==========================================================

@Composable
fun DashboardScreen(

    customers: List<Customer>,

    transactions: List<Transaction>,

    onAddCustomer: () -> Unit,

    onViewCustomers: () -> Unit,

    onViewReports: () -> Unit,

    onBackupRestore: () -> Unit,

    onSignOut: () -> Unit
) {

    // ==================================================
    // TOTAL UDHAAR
    // ==================================================

    val totalUdhar =
        transactions
            .filter {

                it.type.equals(
                    "UDHAR",
                    ignoreCase = true
                )
            }
            .sumOf {

                it.amount
            }


    // ==================================================
    // TOTAL PAYMENT
    // ==================================================

    val totalPayment =
        transactions
            .filter {

                it.type.equals(
                    "PAYMENT",
                    ignoreCase = true
                )
            }
            .sumOf {

                it.amount
            }


    // ==================================================
    // CURRENT BALANCE
    // ==================================================

    val currentBalance =
        totalUdhar - totalPayment


    // ==================================================
    // DASHBOARD
    // ==================================================

    Scaffold(

        floatingActionButton = {

            ExtendedFloatingActionButton(

                onClick =
                    onAddCustomer,

                icon = {

                    Icon(

                        imageVector =
                            Icons.Default.Add,

                        contentDescription =
                            "Add Customer"
                    )
                },

                text = {

                    Text(

                        text =
                            "Add Customer",

                        fontWeight =
                            FontWeight.Bold
                    )
                },

                containerColor =
                    CustomerBlue,

                contentColor =
                    Color.White
            )
        }

    ) { paddingValues ->


        LazyColumn(

            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(
                        paddingValues
                    )
                    .padding(
                        16.dp
                    ),

            verticalArrangement =
                Arrangement.spacedBy(
                    16.dp
                )
        ) {


            // ==================================================
            // HEADER
            // ==================================================

            item {

                Card(

                    modifier =
                        Modifier.fillMaxWidth(),

                    colors =
                        CardDefaults.cardColors(
                            containerColor =
                                CustomerBlue
                        ),

                    elevation =
                        CardDefaults.cardElevation(
                            defaultElevation =
                                8.dp
                        )
                ) {

                    Column(

                        modifier =
                            Modifier.padding(
                                20.dp
                            )
                    ) {

                        Text(

                            text =
                                "AJ Udhar Book",

                            color =
                                Color.White,

                            fontSize =
                                28.sp,

                            fontWeight =
                                FontWeight.Bold
                        )


                        Spacer(
                            modifier =
                                Modifier.height(
                                    5.dp
                                )
                        )


                        Text(

                            text =
                                "Welcome Back 👋",

                            color =
                                Color.White,

                            style =
                                MaterialTheme
                                    .typography
                                    .bodyLarge
                        )


                        Spacer(
                            modifier =
                                Modifier.height(
                                    4.dp
                                )
                        )


                        Text(

                            text =
                                "Manage your customers & transactions",

                            color =
                                Color.White.copy(
                                    alpha =
                                        0.85f
                                ),

                            style =
                                MaterialTheme
                                    .typography
                                    .bodyMedium
                        )
                    }
                }
            }


            // ==================================================
            // TOTAL UDHAAR / PAYMENT
            // ==================================================

            item {

                Row(

                    modifier =
                        Modifier.fillMaxWidth(),

                    horizontalArrangement =
                        Arrangement.spacedBy(
                            12.dp
                        )
                ) {

                    DashboardCard(

                        title =
                            "Total Udhar",

                        value =
                            formatAmount(
                                totalUdhar
                            ),

                        cardColor =
                            UdharGreen,

                        iconText =
                            "💰",

                        modifier =
                            Modifier.weight(
                                1f
                            )
                    )


                    DashboardCard(

                        title =
                            "Received",

                        value =
                            formatAmount(
                                totalPayment
                            ),

                        cardColor =
                            PaymentRed,

                        iconText =
                            "💵",

                        modifier =
                            Modifier.weight(
                                1f
                            )
                    )
                }
            }


            // ==================================================
            // CUSTOMERS / TRANSACTIONS
            // ==================================================

            item {

                Row(

                    modifier =
                        Modifier.fillMaxWidth(),

                    horizontalArrangement =
                        Arrangement.spacedBy(
                            12.dp
                        )
                ) {

                    DashboardCard(

                        title =
                            "Customers",

                        value =
                            customers.size
                                .toString(),

                        cardColor =
                            CustomerBlue,

                        iconText =
                            "👥",

                        modifier =
                            Modifier.weight(
                                1f
                            ),

                        onClick =
                            onViewCustomers
                    )


                    DashboardCard(

                        title =
                            "Transactions",

                        value =
                            transactions.size
                                .toString(),

                        cardColor =
                            TransactionPurple,

                        iconText =
                            "📋",

                        modifier =
                            Modifier.weight(
                                1f
                            )
                    )
                }
            }


            // ==================================================
            // CURRENT BALANCE
            // ==================================================

            item {

                val balanceColor =

                    if (
                        currentBalance > 0
                    ) {

                        UdharGreen

                    } else if (
                        currentBalance < 0
                    ) {

                        PaymentRed

                    } else {

                        BalanceOrange
                    }


                DashboardCard(

                    title =
                        "Current Balance",

                    value =
                        formatAmount(
                            currentBalance
                        ),

                    cardColor =
                        balanceColor,

                    iconText =
                        "💳",

                    modifier =
                        Modifier.fillMaxWidth()
                )
            }


            // ==================================================
            // REPORTS
            // ==================================================

            item {

                ActionDashboardCard(

                    title =
                        "Reports",

                    subtitle =
                        "View Reports",

                    icon =
                        "📊",

                    cardColor =
                        ReportsBlue,

                    onClick =
                        onViewReports
                )
            }


            // ==================================================
            // BACKUP & RESTORE
            // ==================================================

            item {

                ActionDashboardCard(

                    title =
                        "Backup & Restore",

                    subtitle =
                        "Manage Backup",

                    icon =
                        "☁️",

                    cardColor =
                        BackupGreen,

                    onClick =
                        onBackupRestore
                )
            }


            // ==================================================
            // SIGN OUT
            // ==================================================

            item {

                Card(

                    modifier =
                        Modifier.fillMaxWidth(),

                    colors =
                        CardDefaults.cardColors(
                            containerColor =
                                SignOutRed
                        ),

                    elevation =
                        CardDefaults.cardElevation(
                            defaultElevation =
                                7.dp
                        )
                ) {

                    Button(

                        onClick =
                            onSignOut,

                        modifier =
                            Modifier.fillMaxWidth(),

                        colors =
                            androidx.compose.material3.ButtonDefaults
                                .buttonColors(
                                    containerColor =
                                        SignOutRed,

                                    contentColor =
                                        Color.White
                                )
                    ) {

                        Text(

                            text =
                                "🚪  Sign Out",

                            fontSize =
                                18.sp,

                            fontWeight =
                                FontWeight.Bold
                        )
                    }
                }
            }


            // ==================================================
            // RECENT TRANSACTIONS
            // ==================================================

            item {

                Text(

                    text =
                        "Recent Transactions",

                    style =
                        MaterialTheme
                            .typography
                            .titleLarge,

                    fontWeight =
                        FontWeight.Bold
                )
            }


            // ==================================================
            // NO TRANSACTIONS
            // ==================================================

            if (
                transactions.isEmpty()
            ) {

                item {

                    Card(

                        modifier =
                            Modifier.fillMaxWidth(),

                        colors =
                            CardDefaults.cardColors(
                                containerColor =
                                    Color(
                                        0xFFF5F5F5
                                    )
                            ),

                        elevation =
                            CardDefaults.cardElevation(
                                defaultElevation =
                                    3.dp
                            )
                    ) {

                        Column(

                            modifier =
                                Modifier.padding(
                                    20.dp
                                ),

                            horizontalAlignment =
                                Alignment.CenterHorizontally
                        ) {

                            Text(

                                text =
                                    "📭",

                                fontSize =
                                    36.sp
                            )


                            Spacer(
                                modifier =
                                    Modifier.height(
                                        8.dp
                                    )
                            )


                            Text(

                                text =
                                    "No transactions yet.",

                                fontWeight =
                                    FontWeight.Bold
                            )


                            Spacer(
                                modifier =
                                    Modifier.height(
                                        4.dp
                                    )
                            )


                            Text(

                                text =
                                    "Add Udhar or Payment to see history."
                            )
                        }
                    }
                }

            } else {


                // ==================================================
                // RECENT 5 TRANSACTIONS
                // ==================================================

                items(

                    count =
                        minOf(
                            transactions.size,
                            5
                        )

                ) { index ->


                    val transaction =
                        transactions[index]


                    val isUdhar =
                        transaction.type.equals(
                            "UDHAR",
                            ignoreCase =
                                true
                        )


                    val transactionColor =

                        if (
                            isUdhar
                        ) {

                            UdharGreen

                        } else {

                            PaymentRed
                        }


                    Card(

                        modifier =
                            Modifier.fillMaxWidth(),

                        colors =
                            CardDefaults.cardColors(
                                containerColor =
                                    Color.White
                            ),

                        elevation =
                            CardDefaults.cardElevation(
                                defaultElevation =
                                    4.dp
                            )
                    ) {

                        Column(

                            modifier =
                                Modifier.padding(
                                    16.dp
                                )
                        ) {

                            Row(

                                modifier =
                                    Modifier.fillMaxWidth(),

                                horizontalArrangement =
                                    Arrangement.SpaceBetween,

                                verticalAlignment =
                                    Alignment.CenterVertically
                            ) {

                                Text(

                                    text =

                                        if (
                                            isUdhar
                                        ) {

                                            "🟢 Udhar"

                                        } else {

                                            "🔴 Payment"
                                        },

                                    color =
                                        transactionColor,

                                    fontWeight =
                                        FontWeight.Bold,

                                    fontSize =
                                        18.sp
                                )


                                Text(

                                    text =
                                        formatAmount(
                                            transaction.amount
                                        ),

                                    color =
                                        transactionColor,

                                    fontSize =
                                        20.sp,

                                    fontWeight =
                                        FontWeight.Bold
                                )
                            }


                            if (
                                transaction.note
                                    .isNotBlank()
                            ) {

                                Spacer(
                                    modifier =
                                        Modifier.height(
                                            6.dp
                                        )
                                )


                                Text(

                                    text =
                                        transaction.note,

                                    style =
                                        MaterialTheme
                                            .typography
                                            .bodyMedium
                                )
                            }
                        }
                    }
                }
            }


            // ==================================================
            // BOTTOM SPACE
            // ==================================================

            item {

                Spacer(
                    modifier =
                        Modifier.height(
                            60.dp
                        )
                )
            }
        }
    }
}


// ==========================================================
// DASHBOARD CARD
// ==========================================================

@Composable
private fun DashboardCard(

    title: String,

    value: String,

    cardColor: Color,

    iconText: String,

    modifier: Modifier =
        Modifier,

    onClick: () -> Unit =
        {}
) {

    Card(

        modifier =
            modifier,

        onClick =
            onClick,

        colors =
            CardDefaults.cardColors(
                containerColor =
                    cardColor
            ),

        elevation =
            CardDefaults.cardElevation(
                defaultElevation =
                    7.dp
            )
    ) {

        Column(

            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        18.dp
                    ),

            horizontalAlignment =
                Alignment.CenterHorizontally
        ) {

            Text(

                text =
                    iconText,

                fontSize =
                    28.sp
            )


            Spacer(
                modifier =
                    Modifier.height(
                        6.dp
                    )
            )


            Text(

                text =
                    title,

                color =
                    Color.White,

                style =
                    MaterialTheme
                        .typography
                        .bodyMedium,

                fontWeight =
                    FontWeight.Bold
            )


            Spacer(
                modifier =
                    Modifier.height(
                        6.dp
                    )
            )


            Text(

                text =
                    value,

                color =
                    Color.White,

                fontSize =
                    21.sp,

                fontWeight =
                    FontWeight.Bold
            )
        }
    }
}


// ==========================================================
// ACTION CARD
// ==========================================================

@Composable
private fun ActionDashboardCard(

    title: String,

    subtitle: String,

    icon: String,

    cardColor: Color,

    onClick: () -> Unit
) {

    Card(

        modifier =
            Modifier.fillMaxWidth(),

        onClick =
            onClick,

        colors =
            CardDefaults.cardColors(
                containerColor =
                    cardColor
            ),

        elevation =
            CardDefaults.cardElevation(
                defaultElevation =
                    7.dp
            )
    ) {

        Row(

            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        18.dp
                    ),

            verticalAlignment =
                Alignment.CenterVertically
        ) {

            Text(

                text =
                    icon,

                fontSize =
                    34.sp
            )


            Spacer(
                modifier =
                    Modifier.padding(
                        horizontal =
                            10.dp
                    )
            )


            Column {

                Text(

                    text =
                        title,

                    color =
                        Color.White,

                    fontSize =
                        19.sp,

                    fontWeight =
                        FontWeight.Bold
                )


                Spacer(
                    modifier =
                        Modifier.height(
                            3.dp
                        )
                )


                Text(

                    text =
                        subtitle,

                    color =
                        Color.White.copy(
                            alpha =
                                0.9f
                        ),

                    style =
                        MaterialTheme
                            .typography
                            .bodyMedium
                )
            }
        }
    }
}


// ==========================================================
// AMOUNT FORMAT
// ==========================================================

private fun formatAmount(
    amount: Double
): String {

    return "₹%.2f".format(
        Locale.US,
        amount
    )
}