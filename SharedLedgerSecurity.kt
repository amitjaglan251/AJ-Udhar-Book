package com.aj.udharbook.sync

object SharedLedgerSecurity {
    const val PENDING = "PENDING"
    const val APPROVED = "APPROVED"
    const val REJECTED = "REJECTED"

    fun isValidCode(code: String): Boolean = code.matches(Regex("\\d{6}"))
    fun isApproved(status: String): Boolean = status == APPROVED
}
