package com.aj.udharbook.sync

object SyncRetryPolicy {

    fun shouldRetry(
        isSignedIn: Boolean,
        syncSucceeded: Boolean
    ): Boolean = isSignedIn && !syncSucceeded
}
