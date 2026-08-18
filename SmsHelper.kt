package com.aj.udharbook.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.telephony.SmsManager
import androidx.core.content.ContextCompat

object SmsHelper {

    // ==================================================
    // CHECK SMS PERMISSION
    // ==================================================

    fun hasSmsPermission(
        context: Context
    ): Boolean {

        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.SEND_SMS
        ) == PackageManager.PERMISSION_GRANTED
    }


    // ==================================================
    // SEND DIRECT SMS
    // ==================================================

    fun sendSms(
        context: Context,
        mobileNumber: String,
        message: String
    ): Boolean {

        if (mobileNumber.isBlank()) {
            return false
        }

        if (!hasSmsPermission(context)) {
            return false
        }

        return try {

            val cleanNumber =
                mobileNumber
                    .trim()
                    .replace(" ", "")
                    .replace("-", "")

            val smsManager =
                context.getSystemService(
                    SmsManager::class.java
                )

            val parts =
                smsManager.divideMessage(message)

            if (parts.size > 1) {

                smsManager.sendMultipartTextMessage(
                    cleanNumber,
                    null,
                    parts,
                    null,
                    null
                )

            } else {

                smsManager.sendTextMessage(
                    cleanNumber,
                    null,
                    message,
                    null,
                    null
                )
            }

            true

        } catch (e: Exception) {

            e.printStackTrace()

            false
        }
    }

    // ==================================================
    // SEND PAYMENT SMS
    // ==================================================

    fun sendPaymentSms(
        context: Context,
        mobileNumber: String,
        customerName: String,
        paymentAmount: Double,
        remainingBalance: Double
    ) {

        val formattedAmount =
            "₹${"%.2f".format(paymentAmount)}"

        val formattedBalance =
            "₹${"%.2f".format(remainingBalance)}"

        val message = """
AJ DIGITAL POINT

Namaste $customerName ji,

Aapka $formattedAmount ka Payment receive hua hai.

Kul baki: $formattedBalance

Dhanyavaad.
        """.trimIndent()

        sendSms(
            context = context,
            mobileNumber = mobileNumber,
            message = message
        )
    }
}