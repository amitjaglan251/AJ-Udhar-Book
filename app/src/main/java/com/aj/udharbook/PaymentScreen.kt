package com.aj.udharbook.ui.payment

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.aj.udharbook.model.Transaction
import com.aj.udharbook.utils.SmsHelper
import com.aj.udharbook.viewmodel.TransactionViewModel

@Composable
fun PaymentScreen(
    customerId: Int,
    customerName: String,
    customerMobile: String,
    currentBalance: Double,
    transactionViewModel: TransactionViewModel,
    onSaved: () -> Unit
) {
    val context = LocalContext.current

    var paymentAmountText by remember {
        mutableStateOf("")
    }

    var paymentSaved by remember {
        mutableStateOf(false)
    }

    var savedAmount by remember {
        mutableStateOf(0.0)
    }

    var newBalance by remember {
        mutableStateOf(0.0)
    }

    // ==================================================
    // SMS PERMISSION LAUNCHER
    // ==================================================

    val smsPermissionLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->

            if (granted) {

                SmsHelper.sendPaymentSms(
                    context = context,
                    mobileNumber = customerMobile,
                    customerName = customerName,
                    paymentAmount = savedAmount,
                    remainingBalance = newBalance
                )
            }
        }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Top
    ) {
        Text(
            text = "Add Payment"
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Customer: $customerName"
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Mobile: $customerMobile"
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Current Balance: ₹${"%.2f".format(currentBalance)}"
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = paymentAmountText,
            onValueChange = {
                paymentAmountText = it.filter { char ->
                    char.isDigit() || char == '.'
                }
                paymentSaved = false
            },
            label = {
                Text("Payment Amount")
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                val amount = paymentAmountText.toDoubleOrNull()

                if (amount != null && amount > 0) {
                    savedAmount = amount
                    
                    transactionViewModel.insert(
                        transaction = Transaction(
                            customerId = customerId,
                            amount = amount,
                            type = "PAYMENT",
                            note = "Payment Received"
                        ),
                        onCompleted = { updatedBalance ->
                            newBalance = updatedBalance
                            paymentSaved = true
                        }
                    )
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("💾 Save Payment")
        }

        if (paymentSaved) {
            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = {
                    val permissionGranted =
                        ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.SEND_SMS
                        ) == PackageManager.PERMISSION_GRANTED

                    if (permissionGranted) {

                        SmsHelper.sendPaymentSms(
                            context = context,
                            mobileNumber = customerMobile,
                            customerName = customerName,
                            paymentAmount = savedAmount,
                            remainingBalance = newBalance
                        )

                    } else {

                        smsPermissionLauncher.launch(
                            Manifest.permission.SEND_SMS
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("📱 SMS भेजें")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = onSaved,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Back")
        }
    }
}
