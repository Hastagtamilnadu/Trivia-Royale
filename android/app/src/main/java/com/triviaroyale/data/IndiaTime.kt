package com.triviaroyale.data

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

private const val INDIA_TIME_ZONE_ID = "Asia/Kolkata"

object IndiaTime {
    private fun formatter(pattern: String): SimpleDateFormat {
        return SimpleDateFormat(pattern, Locale.US).apply {
            timeZone = TimeZone.getTimeZone(INDIA_TIME_ZONE_ID)
        }
    }

    fun formatDateKey(date: Date = Date()): String = formatter("yyyy-MM-dd").format(date)

    fun formatDateKey(timeMillis: Long): String = formatDateKey(Date(timeMillis))
}
