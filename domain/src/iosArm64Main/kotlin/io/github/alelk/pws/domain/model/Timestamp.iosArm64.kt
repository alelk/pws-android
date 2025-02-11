package io.github.alelk.pws.domain.model

import platform.Foundation.NSCalendar
import platform.Foundation.NSCalendarUnitDay
import platform.Foundation.NSCalendarUnitHour
import platform.Foundation.NSCalendarUnitMinute
import platform.Foundation.NSCalendarUnitMonth
import platform.Foundation.NSCalendarUnitSecond
import platform.Foundation.NSCalendarUnitYear
import platform.Foundation.NSDate

actual fun Timestamp.Companion.now(): Timestamp {
  val calendar = NSCalendar.currentCalendar
  val d = NSDate()
  val components = calendar.components(
    NSCalendarUnitYear or NSCalendarUnitMonth or NSCalendarUnitDay or NSCalendarUnitHour or NSCalendarUnitMinute or NSCalendarUnitSecond,
    d
  )
  val time = Time(components.hour.toInt(), components.minute.toInt(), components.second.toInt())
  val date = Date(Year(components.year.toInt()), Month.of(components.month.toInt()), components.day.toInt())
  return Timestamp(date, time)
}