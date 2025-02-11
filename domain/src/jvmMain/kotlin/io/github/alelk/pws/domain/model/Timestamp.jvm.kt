package io.github.alelk.pws.domain.model

import java.util.Calendar

actual fun Timestamp.Companion.now(): Timestamp {
    val calendar = Calendar.getInstance()
    val time = Time(
        hours = calendar.get(Calendar.HOUR_OF_DAY),
        minutes = calendar.get(Calendar.MINUTE),
        seconds = calendar.get(Calendar.SECOND)
    )
    val date = Date(
        year = Year(calendar.get(Calendar.YEAR)),
        month = Month.of(calendar.get(Calendar.MONTH) + 1),
        dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH)
    )
    return Timestamp(date, time)
}