package no.entur.geocoder.proxy.common

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BlockedResultIdsTest {
    private val blocked = setOf("NSR:StopPlace:64116")

    @Test
    fun `isBlocked is true for a blocked id`() {
        assertTrue(BlockedResultIds.isBlocked("NSR:StopPlace:64116", blocked))
    }

    @Test
    fun `isBlocked is false for a non-blocked id`() {
        assertFalse(BlockedResultIds.isBlocked("NSR:StopPlace:1", blocked))
    }

    @Test
    fun `isBlocked is false for null`() {
        assertFalse(BlockedResultIds.isBlocked(null, blocked))
    }
}
