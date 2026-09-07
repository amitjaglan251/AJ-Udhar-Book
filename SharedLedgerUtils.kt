package com.aj.udharbook.sync

import java.util.Locale
import kotlin.random.Random

object SharedLedgerUtils {
    fun normalizeMobile(value: String): String =
        value.filter(Char::isDigit).takeLast(10)

    fun newInviteCode(): String =
        Random.nextInt(100000, 1000000).toString()

    fun smsMessage(customerName: String, balance: Double, inviteCode: String): String =
        "AJ Udhar Book: $customerName, aapka current balance ₹${String.format(Locale.US, "%.2f", balance)} hai. Shared Ledger join code: $inviteCode"
}
