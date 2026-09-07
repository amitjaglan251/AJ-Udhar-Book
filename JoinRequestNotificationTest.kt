package com.aj.udharbook

import com.aj.udharbook.sync.JoinRequestNotification
import org.junit.Assert.assertEquals
import org.junit.Test

class JoinRequestNotificationTest {
    @Test fun pendingCount_countsOnlyPendingRequests() {
        val requests = listOf(
            JoinRequestNotification("1", "Ledger A", "PENDING"),
            JoinRequestNotification("2", "Ledger A", "APPROVED"),
            JoinRequestNotification("3", "Ledger B", "PENDING")
        )

        assertEquals(2, JoinRequestNotification.pendingCount(requests))
    }

    @Test fun pendingCount_returnsZeroWhenNoRequestsArePending() {
        val requests = listOf(
            JoinRequestNotification("1", "Ledger A", "REJECTED")
        )

        assertEquals(0, JoinRequestNotification.pendingCount(requests))
    }
}
