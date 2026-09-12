package com.aj.udharbook.sync

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SyncRetryPolicyTest {

    @Test
    fun signedOutUserDoesNotRequestRetry() {
        assertFalse(SyncRetryPolicy.shouldRetry(isSignedIn = false, syncSucceeded = false))
    }

    @Test
    fun signedInUserRequestsRetryWhenSyncFails() {
        assertTrue(SyncRetryPolicy.shouldRetry(isSignedIn = true, syncSucceeded = false))
    }

    @Test
    fun successfulSyncDoesNotRequestRetry() {
        assertFalse(SyncRetryPolicy.shouldRetry(isSignedIn = true, syncSucceeded = true))
    }
}
