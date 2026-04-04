package com.triviaroyale.data

import java.util.Calendar

fun isIplSeasonActive(nowMillis: Long = System.currentTimeMillis()): Boolean {
    val calendar = Calendar.getInstance().apply { timeInMillis = nowMillis }
    val month = calendar.get(Calendar.MONTH)
    val day = calendar.get(Calendar.DAY_OF_MONTH)

    return when (month) {
        Calendar.MARCH, Calendar.APRIL -> true
        Calendar.MAY -> day <= 31
        else -> false
    }
}
