package com.aj.udharbook.ui.transaction

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.aj.udharbook.model.Transaction
import com.aj.udharbook.utils.SmsHelper
import com.aj.udharbook.viewmodel.TransactionViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionScreen(
    customerId: Int,
    customerName: String,
    customerMobile: String,
    initialType: String = "UDHAR",
    viewModel: TransactionViewModel,
    onSaved: () -> Unit
) {

    val context = LocalContext.current

    // ==================================================
    // FORM STATE
    // ==================================================

    var amount by remember {
        mutableStateOf("")
    }

    var note by remember {
        mutableStateOf("")
    }

    var type by remember {
        mutableStateOf(initialType)
    }

    // ==================================================
    // PENDING TRANSACTION
    // ==================================================

    var pendingTransaction by remember {
        mutableStateOf<Transaction?>(null)
    }

    // ==================================================
    // SMS PERMISSION RESULT
    // ==================================================

    val smsPermissionLauncher =
        rememberLauncherForActivityResult(
            contract =
                ActivityResultContracts.RequestPermission()
        ) { granted ->

            if (granted) {

                val transaction =
                    pendingTransaction

                if (transaction != null) {

                    saveTransactionAndSendSms(
                        transaction = transaction,
                        customerName = customerName,
                        customerMobile = customerMobile,
                        note = note,
                        viewModel = viewModel,
                        context = context,
                        onSaved = onSaved
                    )
                }

            } else {

                // Permission denied.
                // Transaction फिर भी save होगी,
                // लेकिन SMS नहीं भेजा जाएगा।

                val transaction =
                    pendingTransaction

                if (transaction != null) {

                    saveTransactionOnly(
                        transaction = transaction,
                        viewModel = viewModel,
                        onSaved = onSaved
                    )
                }
            }

            pendingTransaction = null
        }


    // ==================================================
    // SAVE TRANSACTION + SMS
    // ==================================================

    fun processTransaction(
        transaction: Transaction
    ) {

        // --------------------------------------------------
        // SMS number available नहीं है
        // --------------------------------------------------

        if (customerMobile.isBlank()) {

            saveTransactionOnly(
                transaction = transaction,
                viewModel = viewModel,
                onSaved = onSaved
            )

            return
        }


        // --------------------------------------------------
        // Check SMS permission
        // --------------------------------------------------

        val permissionGranted =
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.SEND_SMS
            ) == PackageManager.PERMISSION_GRANTED


        if (permissionGranted) {

            // Permission already granted
            saveTransactionAndSendSms(
                transaction = transaction,
                customerName = customerName,
                customerMobile = customerMobile,
                note = note,
                viewModel = viewModel,
                context = context,
                onSaved = onSaved
            )

        } else {

            // Permission नहीं है
            // Transaction temporarily pending रखेंगे

            pendingTransaction = transaction

            smsPermissionLauncher.launch(
                Manifest.permission.SEND_SMS
            )
        }
    }


    Scaffold(

        topBar = {

            TopAppBar(

                title = {

                    Text(
                        text = "Add Transaction"
                    )
                }
            )
        }

    ) { padding ->

        Column(

            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),

            verticalArrangement =
                Arrangement.spacedBy(16.dp)

        ) {

            // ==================================================
            // CUSTOMER
            // ==================================================

            Card(
                modifier =
                    Modifier.fillMaxWidth()
            ) {

                Column(
                    modifier =
                        Modifier.padding(16.dp)
                ) {

                    Text(
                        text = customerName,
                        fontSize = 22.sp,
                        fontWeight =
                            FontWeight.Bold
                    )

                    Spacer(
                        modifier =
                            Modifier.height(4.dp)
                    )

                    Text(
                        text =
                            "Customer ID : $customerId",
                        style =
                            MaterialTheme
                                .typography
                                .bodyMedium
                    )

                    Spacer(
                        modifier =
                            Modifier.height(4.dp)
                    )

                    Text(
                        text =
                            "Mobile : $customerMobile",
                        style =
                            MaterialTheme
                                .typography
                                .bodyMedium
                    )
                }
            }


            // ==================================================
            // AMOUNT
            // ==================================================

            OutlinedTextField(

                value = amount,

                onValueChange = {
                    amount = it
                },

                label = {
                    Text("Amount")
                },

                keyboardOptions =
                    KeyboardOptions(
                        keyboardType =
                            KeyboardType.Number
                    ),

                modifier =
                    Modifier.fillMaxWidth()
            )


            // ==================================================
            // TRANSACTION TYPE
            // ==================================================

            Text(
                text =
                    "Transaction Type",

                style =
                    MaterialTheme
                        .typography
                        .titleMedium
            )


            Row(

                horizontalArrangement =
                    Arrangement.spacedBy(12.dp)

            ) {

                FilterChip(

                    selected =
                        type == "UDHAR",

                    onClick = {
                        type = "UDHAR"
                    },

                    label = {
                        Text("Udhar")
                    }
                )


                FilterChip(

                    selected =
                        type == "PAYMENT",

                    onClick = {
                        type = "PAYMENT"
                    },

                    label = {
                        Text("Payment")
                    }
                )
            }


            // ==================================================
            // NOTE
            // ==================================================

            OutlinedTextField(

                value = note,

                onValueChange = {
                    note = it
                },

                label = {
                    Text("Note (Optional)")
                },

                modifier =
                    Modifier.fillMaxWidth()
            )


            // ==================================================
            // SAVE BUTTON
            // ==================================================

            Button(

                modifier =
                    Modifier.fillMaxWidth(),

                enabled =
                    amount.isNotBlank(),

                onClick = {

                    val value =
                        amount.toDoubleOrNull()

                    if (
                        value != null &&
                        value > 0
                    ) {

                        // ==================================================
                        // CREATE TRANSACTION
                        // ==================================================

                        val transaction =
                            Transaction(

                                customerId =
                                    customerId,

                                amount =
                                    value,

                                type =
                                    type,

                                note =
                                    note
                            )

                        // ==================================================
                        // PROCESS
                        // ==================================================

                        processTransaction(
                            transaction
                        )
                    }
                }

            ) {

                Text(

                    if (
                        type.equals(
                            "UDHAR",
                            ignoreCase = true
                        )
                    ) {

                        "💾 Save Udhar"

                    } else {

                        "💾 Save Payment"
                    }
                )
            }
        }
    }
}


// ======================================================
// SAVE TRANSACTION ONLY
// ======================================================

private fun saveTransactionOnly(
    transaction: Transaction,
    viewModel: TransactionViewModel,
    onSaved: () -> Unit
) {

    viewModel.insert(
        transaction
    ) {

        onSaved()
    }
}


// ======================================================
// SAVE TRANSACTION + SEND SMS
// ======================================================

private fun saveTransactionAndSendSms(
    transaction: Transaction,
    customerName: String,
    customerMobile: String,
    note: String,
    viewModel: TransactionViewModel,
    context: android.content.Context,
    onSaved: () -> Unit
) {

    viewModel.insert(
        transaction
    ) { newBalance ->

        // ==================================================
        // AMOUNT
        // ==================================================

        val formattedAmount =
            "₹${"%.2f".format(transaction.amount)}"


        // ==================================================
        // BALANCE
        // ==================================================

        val formattedBalance =
            "₹${"%.2f".format(newBalance)}"


        // ==================================================
        // SMS MESSAGE
        // ==================================================

        val message =

            if (
                transaction.type.equals(
                    "UDHAR",
                    ignoreCase = true
                )
            ) {

                """
AJ DIGITAL POINT

Namaste $customerName ji,

Aapke account me $formattedAmount ka Udhar add kiya gaya hai.

Kul baki: $formattedBalance

${if (note.isNotBlank()) "Note: $note\n" else ""}Dhanyavaad.
                """.trimIndent()

            } else {

                """
AJ DIGITAL POINT

Namaste $customerName ji,

Aapka $formattedAmount ka Payment receive hua hai.

Kul baki: $formattedBalance

${if (note.isNotBlank()) "Note: $note\n" else ""}Dhanyavaad.
                """.trimIndent()
            }


        // ==================================================
        // SEND SMS
        // ==================================================

        SmsHelper.sendSms(
            context = context,
            mobileNumber = customerMobile,
            message = message
        )


        // ==================================================
        // BACK
        // ==================================================

        onSaved()
    }
}