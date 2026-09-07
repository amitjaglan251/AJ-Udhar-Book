package com.aj.udharbook

import com.aj.udharbook.sync.SharedLedgerSecurity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SharedLedgerSecurityTest {
    @Test fun pendingRequest_statusIsPending() {
        assertEquals("PENDING", SharedLedgerSecurity.PENDING)
    }

    @Test fun approvedRequest_statusIsApproved() {
        assertTrue(SharedLedgerSecurity.isApproved("APPROVED"))
        assertTrue(!SharedLedgerSecurity.isApproved("PENDING"))
    }

    @Test fun validCode_requiresExactlySixDigits() {
        assertTrue(SharedLedgerSecurity.isValidCode("123456"))
        assertTrue(!SharedLedgerSecurity.isValidCode("12345"))
        assertTrue(!SharedLedgerSecurity.isValidCode("1234567"))
    }
}
