package com.aj.udharbook.ui.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aj.udharbook.model.Customer
import com.aj.udharbook.model.Transaction
import java.util.Locale

private val UdharGreen = Color(0xFF2E7D32)
private val PaymentRed = Color(0xFFD32F2F)
private val CustomerBlue = Color(0xFF1976D2)
private val TransactionPurple = Color(0xFF7B1FA2)
private val BalanceOrange = Color(0xFFEF6C00)
private val ReportsBlue = Color(0xFF1565C0)
private val BackupGreen = Color(0xFF388E3C)
private val SharedBlue = Color(0xFF00695C)
private val SignOutRed = Color(0xFFC62828)

@Composable
fun DashboardScreen(
    customers: List<Customer>,
    transactions: List<Transaction>,
    joinRequestCount: Int = 0,
    onAddCustomer: () -> Unit,
    onViewCustomers: () -> Unit,
    onViewReports: () -> Unit,
    onBackupRestore: () -> Unit,
    onSharedLedger: () -> Unit = {},
    onJoinRequests: () -> Unit = {},
    onSignOut: () -> Unit
) {
    val totalUdhar = transactions.filter { it.type.equals("UDHAR", true) }.sumOf { it.amount }
    val totalPayment = transactions.filter { it.type.equals("PAYMENT", true) }.sumOf { it.amount }
    val currentBalance = totalUdhar - totalPayment

    Scaffold(floatingActionButton = {
        ExtendedFloatingActionButton(
            onClick = onAddCustomer,
            icon = { Text("+") },
            text = { Text("Add Customer", fontWeight = FontWeight.Bold) },
            containerColor = CustomerBlue,
            contentColor = Color.White
        )
    }) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(CustomerBlue), elevation = CardDefaults.cardElevation(8.dp)) {
                    Column(Modifier.padding(20.dp)) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                            Column(Modifier.weight(1f)) {
                                Text("AJ Udhar Book", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.height(5.dp))
                                Text("Welcome Back 👋", color = Color.White, style = MaterialTheme.typography.bodyLarge)
                                Text("Manage your customers & transactions", color = Color.White.copy(alpha = .85f))
                            }
                            if (joinRequestCount > 0) {
                                AssistChip(
                                    onClick = onJoinRequests,
                                    label = { Text("$joinRequestCount", fontWeight = FontWeight.Bold) },
                                    leadingIcon = { Text("🔔") },
                                    colors = AssistChipDefaults.assistChipColors(containerColor = Color.White, labelColor = CustomerBlue)
                                )
                            }
                        }
                    }
                }
            }
            if (joinRequestCount > 0) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = onJoinRequests,
                        colors = CardDefaults.cardColors(MaterialTheme.colorScheme.secondaryContainer),
                        elevation = CardDefaults.cardElevation(4.dp)
                    ) {
                        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("🔔", fontSize = 30.sp)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text("New Join Requests", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                Text("$joinRequestCount pending request${if (joinRequestCount == 1) "" else "s"} waiting for your approval.")
                            }
                            Text("Review ›", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    DashboardCard("Total Udhar", formatAmount(totalUdhar), UdharGreen, "💰", Modifier.weight(1f))
                    DashboardCard("Received", formatAmount(totalPayment), PaymentRed, "💵", Modifier.weight(1f))
                }
            }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    DashboardCard("Customers", customers.size.toString(), CustomerBlue, "👥", Modifier.weight(1f), onViewCustomers)
                    DashboardCard("Transactions", transactions.size.toString(), TransactionPurple, "📋", Modifier.weight(1f))
                }
            }
            item {
                DashboardCard("Current Balance", formatAmount(currentBalance), if (currentBalance > 0) UdharGreen else if (currentBalance < 0) PaymentRed else BalanceOrange, "💳", Modifier.fillMaxWidth())
            }
            item { ActionDashboardCard("Reports", "View Reports", "📊", ReportsBlue, onViewReports) }
            item { ActionDashboardCard("Backup & Restore", "Manage Backup", "☁️", BackupGreen, onBackupRestore) }
            item { ActionDashboardCard("Shared Ledger", "Securely share a customer ledger in real time", "🔗", SharedBlue, onSharedLedger) }
            item {
                Button(onClick = onSignOut, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = SignOutRed)) {
                    Text("🚪  Sign Out", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }
            item { Text("Recent Transactions", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
            if (transactions.isEmpty()) {
                item { Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) { Text("📭", fontSize = 36.sp); Text("No transactions yet.", fontWeight = FontWeight.Bold); Text("Add Udhar or Payment to see history.") } } }
            } else {
                items(minOf(transactions.size, 5)) { index ->
                    val transaction = transactions[index]
                    val isUdhar = transaction.type.equals("UDHAR", true)
                    val color = if (isUdhar) UdharGreen else PaymentRed
                    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(Color.White), elevation = CardDefaults.cardElevation(4.dp)) {
                        Column(Modifier.padding(16.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text(if (isUdhar) "🟢 Udhar" else "🔴 Payment", color = color, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                Text(formatAmount(transaction.amount), color = color, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                            }
                            if (transaction.note.isNotBlank()) { Spacer(Modifier.height(6.dp)); Text(transaction.note) }
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(60.dp)) }
        }
    }
}

@Composable
private fun DashboardCard(title: String, value: String, cardColor: Color, iconText: String, modifier: Modifier = Modifier, onClick: () -> Unit = {}) {
    Card(modifier = modifier, onClick = onClick, colors = CardDefaults.cardColors(cardColor), elevation = CardDefaults.cardElevation(7.dp)) {
        Column(Modifier.fillMaxWidth().padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(iconText, fontSize = 28.sp)
            Spacer(Modifier.height(6.dp))
            Text(title, color = Color.White, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Text(value, color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun ActionDashboardCard(title: String, subtitle: String, icon: String, cardColor: Color, onClick: () -> Unit) {
    Card(Modifier.fillMaxWidth(), onClick = onClick, colors = CardDefaults.cardColors(cardColor), elevation = CardDefaults.cardElevation(7.dp)) {
        Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(icon, fontSize = 34.sp)
            Spacer(Modifier.width(10.dp))
            Column {
                Text(title, color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                Text(subtitle, color = Color.White.copy(alpha = .9f), style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

private fun formatAmount(amount: Double): String = "₹%.2f".format(Locale.US, amount)