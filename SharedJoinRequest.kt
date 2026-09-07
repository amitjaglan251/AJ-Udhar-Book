package com.aj.udharbook.sync

data class SharedJoinRequest(
    val ledgerCode: String,
    val requesterUid: String,
    val requesterName: String,
    val status: String,
    val createdAt: Long
)
