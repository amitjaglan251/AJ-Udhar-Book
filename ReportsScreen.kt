package com.aj.udharbook.ui.reports

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aj.udharbook.model.Customer
import com.aj.udharbook.model.Transaction
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

// ==========================================================
// REPORT COLORS
// ==========================================================

private val UdharGreen = Color(0xFF2E7D32)
private val PaymentRed = Color(0xFFD32F2F)
private val CustomerBlue = Color(0xFF1976D2)
private val TransactionPurple = Color(0xFF7B1FA2)
private val BalanceOrange = Color(0xFFEF6C00)
private val HeaderBlue = Color(0xFF1565C0)
private val LightGreen = Color(0xFFE8F5E9)
private val LightRed = Color(0xFFFFEBEE)
private val LightBlue = Color(0xFFE3F2FD)
private val LightPurple = Color(0xFFF3E5F5)
private val LightOrange = Color(0xFFFFF3E0)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    customers: List<Customer>,
    transactions: List<Transaction>,
    onCustomerClick: (Int) -> Unit
) {

    // ==================================================
    // TOTAL UDHAAR
    // ==================================================

    val totalUdhar = transactions
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

    val totalPayment = transactions
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

    val balanceColor =
        when {
            currentBalance > 0 -> UdharGreen
            currentBalance < 0 -> PaymentRed
            else -> BalanceOrange
        }

    // ==================================================
    // TODAY DATE RANGE
    // ==================================================

    val calendar = Calendar.getInstance()

    calendar.set(
        Calendar.HOUR_OF_DAY,
        0
    )

    calendar.set(
        Calendar.MINUTE,
        0
    )

    calendar.set(
        Calendar.SECOND,
        0
    )

    calendar.set(
        Calendar.MILLISECOND,
        0
    )

    val startOfToday =
        calendar.timeInMillis

    calendar.add(
        Calendar.DAY_OF_YEAR,
        1
    )

    val startOfTomorrow =
        calendar.timeInMillis

    // ==================================================
    // TODAY TRANSACTIONS
    // ==================================================

    val todayTransactions =
        transactions.filter {
            it.timestamp >= startOfToday &&
                    it.timestamp < startOfTomorrow
        }

    val todayUdhar =
        todayTransactions
            .filter {
                it.type.equals(
                    "UDHAR",
                    ignoreCase = true
                )
            }
            .sumOf {
                it.amount
            }

    val todayPayment =
        todayTransactions
            .filter {
                it.type.equals(
                    "PAYMENT",
                    ignoreCase = true
                )
            }
            .sumOf {
                it.amount
            }

    val todayBalance =
        todayUdhar - todayPayment

    val todayBalanceColor =
        when {
            todayBalance > 0 -> UdharGreen
            todayBalance < 0 -> PaymentRed
            else -> BalanceOrange
        }

    // ==================================================
    // UI
    // ==================================================

    Scaffold(

        topBar = {

            TopAppBar(

                title = {

                    Column {

                        Text(
                            text =
                                "Reports",

                            color =
                                Color.White,

                            fontWeight =
                                FontWeight.Bold,

                            fontSize = 21.sp
                        )

                        Text(
                            text =
                                "AJ Udhar Book",

                            color =
                                Color.White.copy(
                                    alpha = 0.85f
                                ),

                            fontSize = 12.sp
                        )
                    }
                },

                colors =
                    TopAppBarDefaults.topAppBarColors(
                        containerColor =
                            HeaderBlue
                    )
            )
        }

    ) { paddingValues ->

        LazyColumn(

            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),

            verticalArrangement =
                Arrangement.spacedBy(16.dp)
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
                                HeaderBlue
                        ),

                    elevation =
                        CardDefaults.cardElevation(
                            defaultElevation = 7.dp
                        )
                ) {

                    Column(
                        modifier =
                            Modifier.padding(20.dp)
                    ) {

                        Text(
                            text =
                                "📊 AJ Udhar Book Reports",

                            color =
                                Color.White,

                            fontSize = 24.sp,

                            fontWeight =
                                FontWeight.Bold
                        )

                        Spacer(
                            modifier =
                                Modifier.height(6.dp)
                        )

                        Text(
                            text =
                                "आपके पूरे हिसाब की जानकारी",

                            color =
                                Color.White.copy(
                                    alpha = 0.9f
                                ),

                            fontSize = 15.sp
                        )
                    }
                }
            }

            // ==================================================
            // MAIN SUMMARY
            // ==================================================

            item {

                Row(

                    modifier =
                        Modifier.fillMaxWidth(),

                    horizontalArrangement =
                        Arrangement.spacedBy(12.dp)
                ) {

                    ReportCard(
                        title =
                            "Total Udhar",

                        value =
                            formatAmount(
                                totalUdhar
                            ),

                        cardColor =
                            LightGreen,

                        textColor =
                            UdharGreen,

                        icon =
                            "🟢",

                        modifier =
                            Modifier.weight(1f)
                    )

                    ReportCard(
                        title =
                            "Payment",

                        value =
                            formatAmount(
                                totalPayment
                            ),

                        cardColor =
                            LightRed,

                        textColor =
                            PaymentRed,

                        icon =
                            "🔴",

                        modifier =
                            Modifier.weight(1f)
                    )
                }
            }

            // ==================================================
            // CURRENT BALANCE
            // ==================================================

            item {

                ReportCard(

                    title =
                        "Current Balance",

                    value =
                        formatAmount(
                            currentBalance
                        ),

                    cardColor =
                        when {
                            currentBalance > 0 ->
                                LightGreen

                            currentBalance < 0 ->
                                LightRed

                            else ->
                                LightOrange
                        },

                    textColor =
                        balanceColor,

                    icon =
                        "💰",

                    modifier =
                        Modifier.fillMaxWidth()
                )
            }

            // ==================================================
            // CUSTOMERS / TRANSACTIONS
            // ==================================================

            item {

                Row(

                    modifier =
                        Modifier.fillMaxWidth(),

                    horizontalArrangement =
                        Arrangement.spacedBy(12.dp)
                ) {

                    ReportCard(
                        title =
                            "Customers",

                        value =
                            customers.size.toString(),

                        cardColor =
                            LightBlue,

                        textColor =
                            CustomerBlue,

                        icon =
                            "👥",

                        modifier =
                            Modifier.weight(1f)
                    )

                    ReportCard(
                        title =
                            "Transactions",

                        value =
                            transactions.size.toString(),

                        cardColor =
                            LightPurple,

                        textColor =
                            TransactionPurple,

                        icon =
                            "📋",

                        modifier =
                            Modifier.weight(1f)
                    )
                }
            }

            // ==================================================
            // TODAY REPORT HEADER
            // ==================================================

            item {

                Text(
                    text =
                        "📅 Today's Report",

                    style =
                        MaterialTheme.typography
                            .titleLarge,

                    fontWeight =
                        FontWeight.Bold
                )

                Spacer(
                    modifier =
                        Modifier.height(3.dp)
                )

                Text(
                    text =
                        "आज के सभी transactions"
                )
            }

            // ==================================================
            // TODAY UDHAAR / PAYMENT
            // ==================================================

            item {

                Row(

                    modifier =
                        Modifier.fillMaxWidth(),

                    horizontalArrangement =
                        Arrangement.spacedBy(12.dp)
                ) {

                    ReportCard(
                        title =
                            "Today's Udhar",

                        value =
                            formatAmount(
                                todayUdhar
                            ),

                        cardColor =
                            LightGreen,

                        textColor =
                            UdharGreen,

                        icon =
                            "🟢",

                        modifier =
                            Modifier.weight(1f)
                    )

                    ReportCard(
                        title =
                            "Today's Payment",

                        value =
                            formatAmount(
                                todayPayment
                            ),

                        cardColor =
                            LightRed,

                        textColor =
                            PaymentRed,

                        icon =
                            "🔴",

                        modifier =
                            Modifier.weight(1f)
                    )
                }
            }

            // ==================================================
            // TODAY BALANCE
            // ==================================================

            item {

                ReportCard(

                    title =
                        "Today's Balance",

                    value =
                        formatAmount(
                            todayBalance
                        ),

                    cardColor =
                        when {
                            todayBalance > 0 ->
                                LightGreen

                            todayBalance < 0 ->
                                LightRed

                            else ->
                                LightOrange
                        },

                    textColor =
                        todayBalanceColor,

                    icon =
                        "💳",

                    modifier =
                        Modifier.fillMaxWidth()
                )
            }

            // ==================================================
            // CUSTOMER-WISE REPORT
            // ==================================================

            item {

                Text(
                    text =
                        "👥 Customer-wise Report",

                    style =
                        MaterialTheme.typography
                            .titleLarge,

                    fontWeight =
                        FontWeight.Bold
                )

                Spacer(
                    modifier =
                        Modifier.height(4.dp)
                )

                Text(
                    text =
                        "हर ग्राहक का Udhar, Payment और बाकी Balance"
                )
            }

            // ==================================================
            // CUSTOMER LIST
            // ==================================================

            if (customers.isEmpty()) {

                item {

                    EmptyReportCard(
                        text =
                            "No customers found."
                    )
                }

            } else {

                items(
                    count =
                        customers.size
                ) { index ->

                    val customer =
                        customers[index]

                    CustomerReportCard(
                        customer =
                            customer,

                        transactions =
                            transactions,

                        onCustomerClick =
                            onCustomerClick
                    )
                }
            }

            // ==================================================
            // TRANSACTION SUMMARY
            // ==================================================

            item {

                Text(
                    text =
                        "📋 Transaction Summary",

                    style =
                        MaterialTheme.typography
                            .titleLarge,

                    fontWeight =
                        FontWeight.Bold
                )

                Spacer(
                    modifier =
                        Modifier.height(4.dp)
                )

                Text(
                    text =
                        "सभी Udhar और Payment transactions"
                )
            }

            // ==================================================
            // TRANSACTION LIST
            // ==================================================

            if (transactions.isEmpty()) {

                item {

                    EmptyReportCard(
                        text =
                            "No transactions found."
                    )
                }

            } else {

                items(
                    count =
                        transactions.size
                ) { index ->

                    val transaction =
                        transactions[index]

                    TransactionReportCard(
                        transaction =
                            transaction
                    )
                }
            }

            // ==================================================
            // BOTTOM SPACE
            // ==================================================

            item {

                Spacer(
                    modifier =
                        Modifier.height(40.dp)
                )
            }
        }
    }
}


// ==========================================================
// CUSTOMER REPORT CARD
// ==========================================================

@Composable
private fun CustomerReportCard(
    customer: Customer,
    transactions: List<Transaction>,
    onCustomerClick: (Int) -> Unit
) {

    val customerTransactions =
        transactions.filter {
            it.customerId == customer.id
        }

    val customerUdhar =
        customerTransactions
            .filter {
                it.type.equals(
                    "UDHAR",
                    ignoreCase = true
                )
            }
            .sumOf {
                it.amount
            }

    val customerPayment =
        customerTransactions
            .filter {
                it.type.equals(
                    "PAYMENT",
                    ignoreCase = true
                )
            }
            .sumOf {
                it.amount
            }

    val customerBalance =
        customerUdhar - customerPayment

    val balanceColor =
        when {
            customerBalance > 0 ->
                UdharGreen

            customerBalance < 0 ->
                PaymentRed

            else ->
                BalanceOrange
        }

    Card(

        modifier =
            Modifier
                .fillMaxWidth(),

        onClick = {
            onCustomerClick(
                customer.id
            )
        },

        colors =
            CardDefaults.cardColors(
                containerColor =
                    Color.White
            ),

        elevation =
            CardDefaults.cardElevation(
                defaultElevation = 5.dp
            )
    ) {

        Column(
            modifier =
                Modifier.padding(16.dp)
        ) {

            // ==================================================
            // CUSTOMER NAME
            // ==================================================

            Text(
                text =
                    "👤 ${customer.name}",

                color =
                    CustomerBlue,

                fontSize = 20.sp,

                fontWeight =
                    FontWeight.Bold
            )

            // ==================================================
            // MOBILE
            // ==================================================

            if (
                customer.mobile.isNotBlank()
            ) {

                Spacer(
                    modifier =
                        Modifier.height(4.dp)
                )

                Text(
                    text =
                        "📱 ${customer.mobile}"
                )
            }

            Spacer(
                modifier =
                    Modifier.height(12.dp)
            )

            // ==================================================
            // UDHAAR / PAYMENT
            // ==================================================

            Row(

                modifier =
                    Modifier.fillMaxWidth(),

                horizontalArrangement =
                    Arrangement.spacedBy(8.dp)
            ) {

                SmallAmountCard(
                    title =
                        "Udhar",

                    amount =
                        customerUdhar,

                    color =
                        UdharGreen,

                    background =
                        LightGreen,

                    modifier =
                        Modifier.weight(1f)
                )

                SmallAmountCard(
                    title =
                        "Payment",

                    amount =
                        customerPayment,

                    color =
                        PaymentRed,

                    background =
                        LightRed,

                    modifier =
                        Modifier.weight(1f)
                )
            }

            Spacer(
                modifier =
                    Modifier.height(10.dp)
            )

            // ==================================================
            // BALANCE
            // ==================================================

            Card(

                modifier =
                    Modifier.fillMaxWidth(),

                colors =
                    CardDefaults.cardColors(
                        containerColor =
                            when {
                                customerBalance > 0 ->
                                    LightGreen

                                customerBalance < 0 ->
                                    LightRed

                                else ->
                                    LightOrange
                            }
                    )
            ) {

                Row(

                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(12.dp),

                    horizontalArrangement =
                        Arrangement.SpaceBetween
                ) {

                    Text(
                        text =
                            "Balance",

                        fontWeight =
                            FontWeight.Bold,

                        color =
                            balanceColor
                    )

                    Text(
                        text =
                            formatAmount(
                                customerBalance
                            ),

                        fontSize = 18.sp,

                        fontWeight =
                            FontWeight.Bold,

                        color =
                            balanceColor
                    )
                }
            }
        }
    }
}


// ==========================================================
// SMALL AMOUNT CARD
// ==========================================================

@Composable
private fun SmallAmountCard(
    title: String,
    amount: Double,
    color: Color,
    background: Color,
    modifier: Modifier = Modifier
) {

    Card(

        modifier =
            modifier,

        colors =
            CardDefaults.cardColors(
                containerColor =
                    background
            )
    ) {

        Column(
            modifier =
                Modifier.padding(10.dp)
        ) {

            Text(
                text =
                    title,

                color =
                    color,

                fontWeight =
                    FontWeight.Bold
            )

            Spacer(
                modifier =
                    Modifier.height(4.dp)
            )

            Text(
                text =
                    formatAmount(
                        amount
                    ),

                color =
                    color,

                fontWeight =
                    FontWeight.Bold,

                fontSize = 16.sp
            )
        }
    }
}


// ==========================================================
// REPORT CARD
// ==========================================================

@Composable
private fun ReportCard(
    title: String,
    value: String,
    cardColor: Color,
    textColor: Color,
    icon: String,
    modifier: Modifier = Modifier
) {

    Card(

        modifier =
            modifier,

        colors =
            CardDefaults.cardColors(
                containerColor =
                    cardColor
            ),

        elevation =
            CardDefaults.cardElevation(
                defaultElevation = 5.dp
            )
    ) {

        Column(
            modifier =
                Modifier.padding(16.dp)
        ) {

            Text(
                text =
                    icon,

                fontSize =
                    24.sp
            )

            Spacer(
                modifier =
                    Modifier.height(5.dp)
            )

            Text(
                text =
                    title,

                color =
                    textColor,

                style =
                    MaterialTheme.typography
                        .bodyMedium,

                fontWeight =
                    FontWeight.Bold
            )

            Spacer(
                modifier =
                    Modifier.height(6.dp)
            )

            Text(
                text =
                    value,

                color =
                    textColor,

                fontSize = 21.sp,

                fontWeight =
                    FontWeight.Bold
            )
        }
    }
}


// ==========================================================
// TRANSACTION CARD
// ==========================================================

@Composable
private fun TransactionReportCard(
    transaction: Transaction
) {

    val isUdhar =
        transaction.type.equals(
            "UDHAR",
            ignoreCase = true
        )

    val typeText =
        if (isUdhar) {
            "Udhar"
        } else {
            "Payment"
        }

    val transactionColor =
        if (isUdhar) {
            UdharGreen
        } else {
            PaymentRed
        }

    val backgroundColor =
        if (isUdhar) {
            LightGreen
        } else {
            LightRed
        }

    val dateText =
        formatDate(
            transaction.timestamp
        )

    Card(

        modifier =
            Modifier.fillMaxWidth(),

        colors =
            CardDefaults.cardColors(
                containerColor =
                    backgroundColor
            ),

        elevation =
            CardDefaults.cardElevation(
                defaultElevation = 4.dp
            )
    ) {

        Column(
            modifier =
                Modifier.padding(16.dp)
        ) {

            // ==================================================
            // TYPE + AMOUNT
            // ==================================================

            Row(

                modifier =
                    Modifier.fillMaxWidth(),

                horizontalArrangement =
                    Arrangement.SpaceBetween
            ) {

                Text(
                    text =
                        if (isUdhar) {
                            "🟢 Udhar"
                        } else {
                            "🔴 Payment"
                        },

                    color =
                        transactionColor,

                    fontWeight =
                        FontWeight.Bold,

                    fontSize = 18.sp
                )

                Text(
                    text =
                        formatAmount(
                            transaction.amount
                        ),

                    color =
                        transactionColor,

                    fontSize = 20.sp,

                    fontWeight =
                        FontWeight.Bold
                )
            }

            Spacer(
                modifier =
                    Modifier.height(5.dp)
            )

            // ==================================================
            // DATE
            // ==================================================

            Text(
                text =
                    "📅 $dateText",

                style =
                    MaterialTheme.typography
                        .bodySmall
            )

            // ==================================================
            // NOTE
            // ==================================================

            if (
                transaction.note.isNotBlank()
            ) {

                Spacer(
                    modifier =
                        Modifier.height(6.dp)
                )

                Text(
                    text =
                        "📝 ${transaction.note}"
                )
            }
        }
    }
}


// ==========================================================
// EMPTY REPORT CARD
// ==========================================================

@Composable
private fun EmptyReportCard(
    text: String
) {

    Card(

        modifier =
            Modifier.fillMaxWidth(),

        colors =
            CardDefaults.cardColors(
                containerColor =
                    Color(0xFFF5F5F5)
            ),

        elevation =
            CardDefaults.cardElevation(
                defaultElevation = 3.dp
            )
    ) {

        Column(
            modifier =
                Modifier.padding(20.dp)
        ) {

            Text(
                text =
                    "📭",

                fontSize =
                    32.sp
            )

            Spacer(
                modifier =
                    Modifier.height(6.dp)
            )

            Text(
                text =
                    text,

                fontWeight =
                    FontWeight.Bold
            )
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


// ==========================================================
// DATE FORMAT
// ==========================================================

private fun formatDate(
    timestamp: Long
): String {

    val formatter =
        SimpleDateFormat(
            "dd MMM yyyy, hh:mm a",
            Locale.getDefault()
        )

    return formatter.format(
        Date(timestamp)
    )
}
