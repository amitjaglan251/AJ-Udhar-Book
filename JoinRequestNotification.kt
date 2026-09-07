package com.aj.udharbook.sync

data class JoinRequestNotification(
    val requesterUid: String,
    val ledgerCode: String,
    val status: String
) {
    companion object {
        fun pendingCount(requests: List<JoinRequestNotification>): Int =
            requests.count { it.status == SharedLedgerSecurity.PENDING }
    }
}
