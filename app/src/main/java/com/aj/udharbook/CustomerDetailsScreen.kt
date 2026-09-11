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
    onDeleteTransaction: (Transaction) -> Unit = {}
) {

    val context = LocalContext.current

    var showDeleteDialog by remember {
        mutableStateOf(false)
    }

    var transactionToDelete by remember {
        mutableStateOf<Transaction?>(null)
    }

    var transactionToEdit by remember {
        mutableStateOf<Transaction?>(null)
    }

    var pdfFile by remember {
        mutableStateOf<File?>(null)
    }

    var showPdfPreview by remember {
        mutableStateOf(false)
    }

    var isGeneratingPdf by remember {
        mutableStateOf(false)
    }

    var pdfError by remember {
        mutableStateOf<String?>(null)
    }

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

    // ==================================================
    // GENERATE PDF
    // ==================================================

    fun generatePdf() {

        if (isGeneratingPdf) return

        isGeneratingPdf = true
        pdfError = null

        try {

            val file =
                PdfGenerator.generateCustomerPdf(
                    context = context,
                    customerName = customerName,
                    mobile = mobile,
                    address = address,
                    transactions = transactions
                )

            pdfFile = file
            isGeneratingPdf = false
            showPdfPreview = true

        } catch (e: Exception) {

            isGeneratingPdf = false

            pdfError =
                e.message
                    ?: "PDF generate नहीं हो सकी।"
        }
    }

    // ==================================================
    // SCREEN
    // ==================================================

    Scaffold(

        topBar = {

            TopAppBar(

                title = {
                    Text(customerName)
                },

                actions = {

                    TextButton(
                        onClick = onEditCustomer
                    ) {
                        Text("Edit")
                    }

                    TextButton(
                        onClick = {
                            showDeleteDialog = true
                        }
                    ) {
                        Text("Delete")
                    }
                }
            )
        }

    ) { paddingValues ->

        LazyColumn(

            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),

            verticalArrangement =
                Arrangement.spacedBy(16.dp)
        ) {

            // ==================================================
            // CUSTOMER INFORMATION
            // ==================================================

            item {

                Card(
                    modifier =
                        Modifier.fillMaxWidth(),

                    elevation =
                        CardDefaults.cardElevation(
                            defaultElevation = 4.dp
                        )
                ) {

                    Column(
                        modifier =
                            Modifier.padding(16.dp)
                    ) {

                        Text(
                            text = customerName,
                            fontSize = 24.sp,
                            fontWeight =
                                FontWeight.Bold
                        )

                        Spacer(
                            modifier =
                                Modifier.height(8.dp)
                        )

                        Text(
                            text =
                                "Mobile : $mobile"
                        )

                        Spacer(
                            modifier =
                                Modifier.height(4.dp)
                        )

                        Text(
                            text =
                                "Address : $address"
                        )
                    }
                }
            }

            // ==================================================
            // SUMMARY
            // ==================================================

            item {

                Row(

                    modifier =
                        Modifier.fillMaxWidth(),

                    horizontalArrangement =
                        Arrangement.spacedBy(12.dp)
                ) {

                    SummaryCard(
                        title = "Udhar",
                        amount = totalUdhar,
                        titleColor = UdharGreen,
                        amountColor = UdharGreen,
                        modifier =
                            Modifier.weight(1f)
                    )

                    SummaryCard(
                        title = "Payment",
                        amount = totalPayment,
                        titleColor = PaymentRed,
                        amountColor = PaymentRed,
                        modifier =
                            Modifier.weight(1f)
                    )
                }
            }

            // ==================================================
            // CURRENT BALANCE
            // ==================================================

            item {

                val balanceColor =
                    if (currentBalance > 0) {
                        UdharGreen
                    } else if (currentBalance < 0) {
                        PaymentRed
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }

                Card(
                    modifier =
                        Modifier.fillMaxWidth(),

                    elevation =
                        CardDefaults.cardElevation(
                            defaultElevation = 6.dp
                        )
                ) {

                    Column(
                        modifier =
                            Modifier.padding(20.dp)
                    ) {

                        Text(
                            text =
                                "Current Balance",

                            style =
                                MaterialTheme.typography
                                    .titleMedium
                        )

                        Spacer(
                            modifier =
                                Modifier.height(8.dp)
                        )

                        Text(
                            text =
                                formatAmount(
                                    currentBalance
                                ),

                            color =
                                balanceColor,

                            fontSize = 30.sp,

                            fontWeight =
                                FontWeight.Bold
                        )
                    }
                }
            }

            // ==================================================
            // ADD BUTTONS
            // ==================================================

            item {

                Row(

                    modifier =
                        Modifier.fillMaxWidth(),

                    horizontalArrangement =
                        Arrangement.spacedBy(12.dp)
                ) {

                    Button(

                        modifier =
                            Modifier.weight(1f),

                        onClick =
                            onAddUdhar,

                        colors =
                            ButtonDefaults.buttonColors(
                                containerColor =
                                    UdharGreen
                            )
                    ) {

                        Text(
                            "Add Udhar"
                        )
                    }

                    Button(

                        modifier =
                            Modifier.weight(1f),

                        onClick =
                            onAddPayment,

                        colors =
                            ButtonDefaults.buttonColors(
                                containerColor =
                                    PaymentRed
                            )
                    ) {

                        Text(
                            "Add Payment"
                        )
                    }
                }
            }

            // ==================================================
            // PDF BUTTON
            // ==================================================

            item {

                Button(

                    modifier =
                        Modifier.fillMaxWidth(),

                    enabled =
                        !isGeneratingPdf,

                    onClick = {
                        generatePdf()
                    }
                ) {

                    if (isGeneratingPdf) {

                        CircularProgressIndicator(
                            modifier =
                                Modifier.height(20.dp)
                        )

                    } else {

                        Text(
                            "📄 Generate PDF"
                        )
                    }
                }
            }

            // ==================================================
            // PDF ERROR
            // ==================================================

            if (pdfError != null) {

                item {

                    Card(
                        modifier =
                            Modifier.fillMaxWidth()
                    ) {

                        Text(
                            text =
                                pdfError
                                    ?: "",

                            modifier =
                                Modifier.padding(16.dp)
                        )
                    }
                }
            }

            // ==================================================
            // TRANSACTION HISTORY
            // ==================================================

            item {

                Text(
                    text =
                        "Transaction History",

                    style =
                        MaterialTheme.typography
                            .titleLarge,

                    fontWeight =
                        FontWeight.Bold
                )
            }

            // ==================================================
            // EMPTY TRANSACTION
            // ==================================================

            if (transactions.isEmpty()) {

                item {

                    Card(
                        modifier =
                            Modifier.fillMaxWidth()
                    ) {

                        Column(
                            modifier =
                                Modifier.padding(20.dp)
                        ) {

                            Text(
                                text =
                                    "No transactions yet."
                            )

                            Spacer(
                                modifier =
                                    Modifier.height(4.dp)
                            )

                            Text(
                                text =
                                    "Add Udhar or Payment to see history."
                            )
                        }
                    }
                }

            } else {

                items(
                    count =
                        transactions.size
                ) { index ->

                    val transaction =
                        transactions[index]

                    TransactionCard(
                        transaction =
                            transaction,

                        onEdit = {

                            transactionToEdit =
                                transaction
                        },

                        onDelete = {

                            transactionToDelete =
                                transaction
                        }
                    )
                }
            }
        }
    }

    // ==================================================
    // DELETE CUSTOMER CONFIRMATION
    // ==================================================

    if (showDeleteDialog) {

        AlertDialog(

            onDismissRequest = {
                showDeleteDialog = false
            },

            title = {
                Text(
                    "Delete Customer?"
                )
            },

            text = {

                Text(
                    "क्या आप \"$customerName\" को delete करना चाहते हैं?"
                )
            },

            confirmButton = {

                TextButton(

                    onClick = {

                        showDeleteDialog = false

                        onDeleteCustomer()
                    }
                ) {

                    Text(
                        "Delete"
                    )
                }
            },

            dismissButton = {

                TextButton(

                    onClick = {
                        showDeleteDialog = false
                    }
                ) {

                    Text(
                        "Cancel"
                    )
                }
            }
        )
    }

    // ==================================================
    // DELETE TRANSACTION CONFIRMATION
    // ==================================================

    transactionToDelete?.let { transaction ->

        AlertDialog(

            onDismissRequest = {
                transactionToDelete = null
            },

            title = {
                Text("Delete Transaction?")
            },

            text = {

                Text(
                    "क्या आप इस ${transaction.type} transaction को delete करना चाहते हैं?\n\n" +
                            "Amount: ${formatAmount(transaction.amount)}"
                )
            },

            confirmButton = {

                TextButton(

                    onClick = {

                        onDeleteTransaction(
                            transaction
                        )

                        transactionToDelete = null
                    }
                ) {

                    Text("Delete")
                }
            },

            dismissButton = {

                TextButton(

                    onClick = {
                        transactionToDelete = null
                    }
                ) {

                    Text("Cancel")
                }
            }
        )
    }

    // ==================================================
    // EDIT TRANSACTION DIALOG
    // ==================================================

    transactionToEdit?.let { transaction ->

        EditTransactionDialog(

            transaction = transaction,

            onDismiss = {
                transactionToEdit = null
            },

            onSave = { updatedTransaction ->

                onEditTransaction(
                    updatedTransaction
                )

                transactionToEdit = null
            }
        )
    }

    // ==================================================
    // PDF PREVIEW
    // ==================================================

    if (showPdfPreview && pdfFile != null) {

        PdfPreviewDialog(

            file = pdfFile!!,

            onDismiss = {
                showPdfPreview = false
            },

            onShare = {

                sharePdf(
                    context = context,
                    file = pdfFile!!
                )
            }
        )
    }
}


// ==========================================================
// EDIT TRANSACTION DIALOG
// ==========================================================

@Composable
private fun EditTransactionDialog(
    transaction: Transaction,
    onDismiss: () -> Unit,
    onSave: (Transaction) -> Unit
) {

    val context = LocalContext.current

    val calendar =
        remember(transaction.id) {

            Calendar.getInstance().apply {
                timeInMillis =
                    transaction.timestamp
            }
        }

    var selectedDate by remember(
        transaction.id
    ) {

        mutableStateOf(
            SimpleDateFormat(
                "dd MMM yyyy",
                Locale.getDefault()
            ).format(
                Date(
                    transaction.timestamp
                )
            )
        )
    }

    var selectedTime by remember(
        transaction.id
    ) {

        mutableStateOf(
            SimpleDateFormat(
                "hh:mm a",
                Locale.getDefault()
            ).format(
                Date(
                    transaction.timestamp
                )
            )
        )
    }

    var amountText by remember(
        transaction.id
    ) {

        mutableStateOf(
            transaction.amount.toString()
        )
    }

    var noteText by remember(
        transaction.id
    ) {

        mutableStateOf(
            transaction.note
        )
    }

    var type by remember(
        transaction.id
    ) {

        mutableStateOf(
            transaction.type
        )
    }

    var errorMessage by remember {
        mutableStateOf<String?>(null)
    }

    fun showDatePicker() {

        val datePicker =
            DatePickerDialog(

                context,

                { _, year, month, dayOfMonth ->

                    calendar.set(
                        Calendar.YEAR,
                        year
                    )

                    calendar.set(
                        Calendar.MONTH,
                        month
                    )

                    calendar.set(
                        Calendar.DAY_OF_MONTH,
                        dayOfMonth
                    )

                    selectedDate =
                        SimpleDateFormat(
                            "dd MMM yyyy",
                            Locale.getDefault()
                        ).format(
                            calendar.time
                        )
                },

                calendar.get(
                    Calendar.YEAR
                ),

                calendar.get(
                    Calendar.MONTH
                ),

                calendar.get(
                    Calendar.DAY_OF_MONTH
                )
            )

        datePicker.show()
    }

    fun showTimePicker() {

        val timePicker =
            TimePickerDialog(

                context,

                { _, hourOfDay, minute ->

                    calendar.set(
                        Calendar.HOUR_OF_DAY,
                        hourOfDay
                    )

                    calendar.set(
                        Calendar.MINUTE,
                        minute
                    )

                    calendar.set(
                        Calendar.SECOND,
                        0
                    )

                    calendar.set(
                        Calendar.MILLISECOND,
                        0
                    )

                    selectedTime =
                        SimpleDateFormat(
                            "hh:mm a",
                            Locale.getDefault()
                        ).format(
                            calendar.time
                        )
                },

                calendar.get(
                    Calendar.HOUR_OF_DAY
                ),

                calendar.get(
                    Calendar.MINUTE
                ),

                false
            )

        timePicker.show()
    }

    AlertDialog(

        onDismissRequest = onDismiss,

        title = {
            Text("Edit Transaction")
        },

        text = {

            Column {

                Text(
                    text =
                        "Transaction Type",

                    fontWeight =
                        FontWeight.Bold
                )

                Spacer(
                    modifier =
                        Modifier.height(8.dp)
                )

                Row(
                    horizontalArrangement =
                        Arrangement.spacedBy(8.dp)
                ) {

                    if (
                        type.equals(
                            "UDHAR",
                            ignoreCase = true
                        )
                    ) {

                        Button(
                            onClick = {
                                type = "UDHAR"
                            },

                            colors =
                                ButtonDefaults.buttonColors(
                                    containerColor =
                                        UdharGreen
                                )
                        ) {

                            Text(
                                "Udhar"
                            )
                        }

                        OutlinedButton(
                            onClick = {
                                type = "PAYMENT"
                            }
                        ) {

                            Text(
                                "Payment",
                                color = PaymentRed
                            )
                        }

                    } else {

                        OutlinedButton(
                            onClick = {
                                type = "UDHAR"
                            }
                        ) {

                            Text(
                                "Udhar",
                                color = UdharGreen
                            )
                        }

                        Button(
                            onClick = {
                                type = "PAYMENT"
                            },

                            colors =
                                ButtonDefaults.buttonColors(
                                    containerColor =
                                        PaymentRed
                                )
                        ) {

                            Text(
                                "Payment"
                            )
                        }
                    }
                }

                Spacer(
                    modifier =
                        Modifier.height(12.dp)
                )

                OutlinedTextField(

                    value =
                        amountText,

                    onValueChange = {

                        amountText = it

                        errorMessage = null
                    },

                    label = {
                        Text("Amount")
                    },

                    singleLine = true,

                    modifier =
                        Modifier.fillMaxWidth()
                )

                Spacer(
                    modifier =
                        Modifier.height(12.dp)
                )

                OutlinedButton(

                    onClick = {
                        showDatePicker()
                    },

                    modifier =
                        Modifier.fillMaxWidth()
                ) {

                    Text(
                        "📅 Date: $selectedDate"
                    )
                }

                Spacer(
                    modifier =
                        Modifier.height(8.dp)
                )

                OutlinedButton(

                    onClick = {
                        showTimePicker()
                    },

                    modifier =
                        Modifier.fillMaxWidth()
                ) {

                    Text(
                        "🕐 Time: $selectedTime"
                    )
                }

                Spacer(
                    modifier =
                        Modifier.height(12.dp)
                )

                OutlinedTextField(

                    value =
                        noteText,

                    onValueChange = {
                        noteText = it
                    },

                    label = {
                        Text("Note")
                    },

                    modifier =
                        Modifier.fillMaxWidth()
                )

                if (errorMessage != null) {

                    Spacer(
                        modifier =
                            Modifier.height(8.dp)
                    )

                    Text(
                        text =
                            errorMessage!!,

                        color =
                            MaterialTheme
                                .colorScheme
                                .error
                    )
                }
            }
        },

        confirmButton = {

            TextButton(

                onClick = {

                    val amount =
                        amountText.toDoubleOrNull()

                    if (
                        amount == null ||
                        amount <= 0
                    ) {

                        errorMessage =
                            "Valid amount डालें।"

                        return@TextButton
                    }

                    val updatedTransaction =
                        transaction.copy(

                            amount =
                                amount,

                            type =
                                type,

                            note =
                                noteText,

                            timestamp =
                                calendar.timeInMillis
                        )

                    onSave(
                        updatedTransaction
                    )
                }
            ) {

                Text(
                    "Save"
                )
            }
        },

        dismissButton = {

            TextButton(
                onClick = onDismiss
            ) {

                Text(
                    "Cancel"
                )
            }
        }
    )
}


// ==========================================================
// PDF PREVIEW DIALOG
// ==========================================================

@Composable
private fun PdfPreviewDialog(
    file: File,
    onDismiss: () -> Unit,
    onShare: () -> Unit
) {

    var bitmap by remember {
        mutableStateOf<Bitmap?>(null)
    }

    LaunchedEffect(file) {

        try {

            val descriptor =
                ParcelFileDescriptor.open(
                    file,
                    ParcelFileDescriptor.MODE_READ_ONLY
                )

            val renderer =
                PdfRenderer(descriptor)

            if (renderer.pageCount > 0) {

                val page =
                    renderer.openPage(0)

                val pageBitmap =
                    Bitmap.createBitmap(
                        page.width * 2,
                        page.height * 2,
                        Bitmap.Config.ARGB_8888
                    )

                page.render(
                    pageBitmap,
                    null,
                    null,
                    PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY
                )

                page.close()
                renderer.close()
                descriptor.close()

                bitmap = pageBitmap
            }

        } catch (_: Exception) {

            bitmap = null
        }
    }

    AlertDialog(

        onDismissRequest = onDismiss,

        title = {
            Text(
                "PDF Preview"
            )
        },

        text = {

            Box(
                modifier =
                    Modifier.fillMaxWidth(),

                contentAlignment =
                    Alignment.Center
            ) {

                if (bitmap != null) {

                    Image(
                        bitmap =
                            bitmap!!.asImageBitmap(),

                        contentDescription =
                            "PDF Preview",

                        modifier =
                            Modifier.fillMaxWidth()
                    )

                } else {

                    CircularProgressIndicator()
                }
            }
        },

        confirmButton = {

            TextButton(
                onClick = onShare
            ) {

                Text(
                    "Share"
                )
            }
        },

        dismissButton = {

            TextButton(
                onClick = onDismiss
            ) {

                Text(
                    "Close"
                )
            }
        }
    )
}


// ==========================================================
// SHARE PDF
// ==========================================================

private fun sharePdf(
    context: Context,
    file: File
) {

    try {

        val uri: Uri =
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

        val intent =
            Intent(
                Intent.ACTION_SEND
            ).apply {

                type =
                    "application/pdf"

                putExtra(
                    Intent.EXTRA_STREAM,
                    uri
                )

                addFlags(
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }

        context.startActivity(
            Intent.createChooser(
                intent,
                "Share PDF"
            )
        )

    } catch (e: Exception) {

        e.printStackTrace()
    }
}


// ==========================================================
// SUMMARY CARD
// ==========================================================

@Composable
private fun SummaryCard(
    title: String,
    amount: Double,
    titleColor: Color,
    amountColor: Color,
    modifier: Modifier = Modifier
) {

    Card(

        modifier = modifier,

        elevation =
            CardDefaults.cardElevation(
                defaultElevation = 4.dp
            )
    ) {

        Column(
            modifier =
                Modifier.padding(16.dp)
        ) {

            Text(
                text =
                    title,

                color =
                    titleColor,

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
                    formatAmount(
                        amount
                    ),

                color =
                    amountColor,

                fontSize =
                    20.sp,

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
private fun TransactionCard(
    transaction: Transaction,
    onEdit: () -> Unit,
    onDelete: () -> Unit
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

    val dateText =
        formatDate(
            transaction.timestamp
        )

    Card(

        modifier =
            Modifier.fillMaxWidth(),

        elevation =
            CardDefaults.cardElevation(
                defaultElevation = 3.dp
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
                    Arrangement.SpaceBetween,

                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                Text(
                    text =
                        typeText,

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

            Spacer(
                modifier =
                    Modifier.height(6.dp)
            )

            Text(
                text =
                    dateText,

                style =
                    MaterialTheme.typography
                        .bodySmall
            )

            if (
                transaction.note.isNotBlank()
            ) {

                Spacer(
                    modifier =
                        Modifier.height(6.dp)
                )

                Text(
                    text =
                        "Note: ${transaction.note}"
                )
            }

            Spacer(
                modifier =
                    Modifier.height(12.dp)
            )

            Row(

                modifier =
                    Modifier.fillMaxWidth(),

                horizontalArrangement =
                    Arrangement.End,

                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                OutlinedButton(
                    onClick = onEdit
                ) {

                    Text(
                        "✏️ Edit"
                    )
                }

                Spacer(
                    modifier =
                        Modifier.padding(
                            horizontal = 4.dp
                        )
                )

                TextButton(
                    onClick = onDelete
                ) {

                    Text(
                        "🗑 Delete",

                        color =
                            MaterialTheme
                                .colorScheme
                                .error
                    )
                }
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
