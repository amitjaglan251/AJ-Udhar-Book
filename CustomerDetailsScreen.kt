package com.aj.udharbook.ui.customer

import android.graphics.Bitmap
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.aj.udharbook.model.Transaction
import com.aj.udharbook.pdf.PdfGenerator
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private val UdharGreen = Color(0xFF2E7D32)
private val PaymentRed = Color(0xFFD32F2F)
private val SharedBlue = Color(0xFF00695C)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerDetailsScreen(
    customerName: String,
    mobile: String,
    address: String,
    transactions: List<Transaction>,
    onAddUdhar: () -> Unit,
    onAddPayment: () -> Unit,
    onEditCustomer: () -> Unit = {},
    onDeleteCustomer: () -> Unit = {},
    onEditTransaction: (Transaction) -> Unit = {},
    onDeleteTransaction: (Transaction) -> Unit = {},
    onSmsCustomer: () -> Unit = {},
    sharedLedgerId: String = ""
) {
    val context = LocalContext.current
    var showDeleteDialog by remember { mutableStateOf(false) }
    var transactionToDelete by remember { mutableStateOf<Transaction?>(null) }
    var transactionToEdit by remember { mutableStateOf<Transaction?>(null) }
    var pdfFile by remember { mutableStateOf<File?>(null) }
    var showPdfPreview by remember { mutableStateOf(false) }
    var isGeneratingPdf by remember { mutableStateOf(false) }
    var pdfError by remember { mutableStateOf<String?>(null) }

    val totalUdhar = transactions.filter { it.type.equals("UDHAR", true) }.sumOf { it.amount }
    val totalPayment = transactions.filter { it.type.equals("PAYMENT", true) }.sumOf { it.amount }
    val currentBalance = totalUdhar - totalPayment

    fun generatePdf() {
        if (isGeneratingPdf) return
        isGeneratingPdf = true
        pdfError = null
        try {
            pdfFile = PdfGenerator.generateCustomerPdf(
                context = context,
                customerName = customerName,
                mobile = mobile,
                address = address,
                transactions = transactions
            )
            isGeneratingPdf = false
            showPdfPreview = true
        } catch (e: Exception) {
            isGeneratingPdf = false
            pdfError = e.message ?: "PDF generate नहीं हो सकी।"
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(customerName) },
                actions = {
                    TextButton(onClick = onEditCustomer) { Text("Edit") }
                    TextButton(onClick = { showDeleteDialog = true }) { Text("Delete") }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(customerName, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        Text("Mobile : $mobile")
                        Spacer(Modifier.height(4.dp))
                        Text("Address : $address")
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (sharedLedgerId.isNotBlank())
                            SharedBlue.copy(alpha = 0.10f)
                        else MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            if (sharedLedgerId.isNotBlank()) "🔗 Shared Ledger Active"
                            else "🔗 Shared Ledger Not Linked",
                            color = if (sharedLedgerId.isNotBlank()) SharedBlue else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Spacer(Modifier.height(6.dp))
                        if (sharedLedgerId.isNotBlank()) {
                            Text("Join Code: $sharedLedgerId")
                            Text("Transactions sync automatically between participants.")
                        } else {
                            Text("SMS Customer दबाकर shared ledger code बनाएं और customer को भेजें।")
                        }
                    }
                }
            }

            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    SummaryCard("Udhar", totalUdhar, UdharGreen, UdharGreen, Modifier.weight(1f))
                    SummaryCard("Payment", totalPayment, PaymentRed, PaymentRed, Modifier.weight(1f))
                }
            }

            item {
                val balanceColor = when {
                    currentBalance > 0 -> UdharGreen
                    currentBalance < 0 -> PaymentRed
                    else -> MaterialTheme.colorScheme.onSurface
                }
                Card(Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)) {
                    Column(Modifier.padding(20.dp)) {
                        Text("Current Balance", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(8.dp))
                        Text(formatAmount(currentBalance), color = balanceColor, fontSize = 30.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        modifier = Modifier.weight(1f),
                        onClick = onAddUdhar,
                        colors = ButtonDefaults.buttonColors(containerColor = UdharGreen)
                    ) { Text("Add Udhar") }
                    Button(
                        modifier = Modifier.weight(1f),
                        onClick = onAddPayment,
                        colors = ButtonDefaults.buttonColors(containerColor = PaymentRed)
                    ) { Text("Add Payment") }
                }
            }

            item {
                Button(modifier = Modifier.fillMaxWidth(), onClick = onSmsCustomer) {
                    Text("📩 SMS Customer")
                }
            }

            item {
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isGeneratingPdf,
                    onClick = ::generatePdf
                ) {
                    if (isGeneratingPdf) CircularProgressIndicator(modifier = Modifier.height(20.dp))
                    else Text("📄 Generate PDF")
                }
            }

            if (pdfError != null) {
                item {
                    Card(Modifier.fillMaxWidth()) {
                        Text(pdfError ?: "", Modifier.padding(16.dp))
                    }
                }
            }

            item {
                Text("Transaction History", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }

            if (transactions.isEmpty()) {
                item {
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(20.dp)) {
                            Text("No transactions yet.")
                            Spacer(Modifier.height(4.dp))
                            Text("Add Udhar or Payment to see history.")
                        }
                    }
                }
            } else {
                items(transactions.size) { index ->
                    val transaction = transactions[index]
                    TransactionCard(
                        transaction = transaction,
                        onEdit = { transactionToEdit = transaction },
                        onDelete = { transactionToDelete = transaction }
                    )
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Customer?") },
            text = { Text("क्या आप \"$customerName\" को delete करना चाहते हैं?") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    onDeleteCustomer()
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            }
        )
    }

    transactionToDelete?.let { transaction ->
        AlertDialog(
            onDismissRequest = { transactionToDelete = null },
            title = { Text("Delete Transaction?") },
            text = { Text("क्या आप इस ${transaction.type} transaction को delete करना चाहते हैं?\n\nAmount: ${formatAmount(transaction.amount)}") },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteTransaction(transaction)
                    transactionToDelete = null
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { transactionToDelete = null }) { Text("Cancel") }
            }
        )
    }

    transactionToEdit?.let { transaction ->
        EditTransactionDialog(
            transaction = transaction,
            onDismiss = { transactionToEdit = null },
            onSave = {
                onEditTransaction(it)
                transactionToEdit = null
            }
        )
    }

    if (showPdfPreview && pdfFile != null) {
        PdfPreviewDialog(
            file = pdfFile!!,
            onDismiss = { showPdfPreview = false },
            onShare = { sharePdf(context, pdfFile!!) }
        )
    }
}

@Composable
private fun EditTransactionDialog(transaction: Transaction, onDismiss: () -> Unit, onSave: (Transaction) -> Unit) {
    val context = LocalContext.current
    val calendar = remember(transaction.id) { Calendar.getInstance().apply { timeInMillis = transaction.timestamp } }
    var selectedDate by remember(transaction.id) { mutableStateOf(SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(transaction.timestamp))) }
    var selectedTime by remember(transaction.id) { mutableStateOf(SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(transaction.timestamp))) }
    var amountText by remember(transaction.id) { mutableStateOf(transaction.amount.toString()) }
    var noteText by remember(transaction.id) { mutableStateOf(transaction.note) }
    var type by remember(transaction.id) { mutableStateOf(transaction.type) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun showDatePicker() {
        DatePickerDialog(context, { _, year, month, dayOfMonth ->
            calendar.set(Calendar.YEAR, year); calendar.set(Calendar.MONTH, month); calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
            selectedDate = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(calendar.time)
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
    }

    fun showTimePicker() {
        TimePickerDialog(context, { _, hourOfDay, minute ->
            calendar.set(Calendar.HOUR_OF_DAY, hourOfDay); calendar.set(Calendar.MINUTE, minute); calendar.set(Calendar.SECOND, 0); calendar.set(Calendar.MILLISECOND, 0)
            selectedTime = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(calendar.time)
        }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), false).show()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Transaction") },
        text = {
            Column {
                Text("Transaction Type", fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (type.equals("UDHAR", true)) {
                        Button(onClick = { type = "UDHAR" }, colors = ButtonDefaults.buttonColors(containerColor = UdharGreen)) { Text("Udhar") }
                        OutlinedButton(onClick = { type = "PAYMENT" }) { Text("Payment", color = PaymentRed) }
                    } else {
                        OutlinedButton(onClick = { type = "UDHAR" }) { Text("Udhar", color = UdharGreen) }
                        Button(onClick = { type = "PAYMENT" }, colors = ButtonDefaults.buttonColors(containerColor = PaymentRed)) { Text("Payment") }
                    }
                }
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(amountText, { amountText = it; errorMessage = null }, label = { Text("Amount") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(12.dp))
                OutlinedButton(onClick = ::showDatePicker, modifier = Modifier.fillMaxWidth()) { Text("📅 Date: $selectedDate") }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = ::showTimePicker, modifier = Modifier.fillMaxWidth()) { Text("🕐 Time: $selectedTime") }
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(noteText, { noteText = it }, label = { Text("Note") }, modifier = Modifier.fillMaxWidth())
                if (errorMessage != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(errorMessage!!, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val amount = amountText.toDoubleOrNull()
                if (amount == null || amount <= 0) {
                    errorMessage = "Valid amount डालें।"
                    return@TextButton
                }
                onSave(transaction.copy(amount = amount, type = type, note = noteText, timestamp = calendar.timeInMillis))
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun PdfPreviewDialog(file: File, onDismiss: () -> Unit, onShare: () -> Unit) {
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(file) {
        try {
            val descriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            val renderer = PdfRenderer(descriptor)
            if (renderer.pageCount > 0) {
                val page = renderer.openPage(0)
                val pageBitmap = Bitmap.createBitmap(page.width * 2, page.height * 2, Bitmap.Config.ARGB_8888)
                page.render(pageBitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                page.close(); renderer.close(); descriptor.close()
                bitmap = pageBitmap
            }
        } catch (_: Exception) { bitmap = null }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("PDF Preview") },
        text = {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                if (bitmap != null) Image(bitmap!!.asImageBitmap(), "PDF Preview", Modifier.fillMaxWidth())
                else CircularProgressIndicator()
            }
        },
        confirmButton = { TextButton(onClick = onShare) { Text("Share") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}

private fun sharePdf(context: Context, file: File) {
    try {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share PDF"))
    } catch (e: Exception) { e.printStackTrace() }
}

@Composable
private fun SummaryCard(title: String, amount: Double, titleColor: Color, amountColor: Color, modifier: Modifier = Modifier) {
    Card(modifier = modifier, elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)) {
        Column(Modifier.padding(16.dp)) {
            Text(title, color = titleColor, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Text(formatAmount(amount), color = amountColor, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun TransactionCard(transaction: Transaction, onEdit: () -> Unit, onDelete: () -> Unit) {
    val isUdhar = transaction.type.equals("UDHAR", true)
    val typeText = if (isUdhar) "Udhar" else "Payment"
    val transactionColor = if (isUdhar) UdharGreen else PaymentRed
    Card(Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(typeText, color = transactionColor, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text(formatAmount(transaction.amount), color = transactionColor, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(6.dp))
            Text(formatDate(transaction.timestamp), style = MaterialTheme.typography.bodySmall)
            if (transaction.note.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Text("Note: ${transaction.note}")
            }
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(onClick = onEdit) { Text("✏️ Edit") }
                Spacer(Modifier.padding(horizontal = 4.dp))
                TextButton(onClick = onDelete) { Text("🗑 Delete", color = MaterialTheme.colorScheme.error) }
            }
        }
    }
}

private fun formatAmount(amount: Double): String = "₹%.2f".format(Locale.US, amount)

private fun formatDate(timestamp: Long): String = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(timestamp))
