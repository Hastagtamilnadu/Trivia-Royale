package com.triviaroyale.data

import java.time.Instant
import java.util.Date
import org.junit.Assert.assertEquals
import org.junit.Test

class IndiaTimeTest {
    @Test
    fun `date key rolls over at india midnight`() {
        assertEquals(
            "2026-04-01",
            IndiaTime.formatDateKey(Date.from(Instant.parse("2026-04-01T18:29:59Z")))
        )
        assertEquals(
            "2026-04-02",
            IndiaTime.formatDateKey(Date.from(Instant.parse("2026-04-01T18:30:00Z")))
        )
    }

    @Test
    fun `date key uses india timezone for utc evening`() {
        assertEquals(
            "2026-04-01",
            IndiaTime.formatDateKey(Date.from(Instant.parse("2026-03-31T23:00:00Z")))
        )
    }
}
