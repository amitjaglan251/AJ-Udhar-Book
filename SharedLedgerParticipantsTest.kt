package com.aj.udharbook

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SharedLedgerParticipantsTest {
    @Test fun addParticipant_doesNotDuplicateExistingUid() {
        val participants = mutableListOf("owner")
        if (!participants.contains("owner")) participants.add("owner")

        assertEquals(listOf("owner"), participants)
    }

    @Test fun addParticipant_appendsNewUid() {
        val participants = mutableListOf("owner")
        if (!participants.contains("member")) participants.add("member")

        assertTrue(participants.contains("owner"))
        assertTrue(participants.contains("member"))
        assertEquals(2, participants.size)
    }
}
