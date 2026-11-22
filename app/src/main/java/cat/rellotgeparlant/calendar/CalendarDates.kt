package cat.rellotgeparlant.calendar

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset

/** Converts CalendarContract instance times. All-day instances are stored at UTC midnight. */
object CalendarDates {
    fun allDayDate(utcMillis: Long): LocalDate =
        Instant.ofEpochMilli(utcMillis).atZone(ZoneOffset.UTC).toLocalDate()

    fun timedStart(millis: Long, zone: ZoneId): LocalDateTime =
        Instant.ofEpochMilli(millis).atZone(zone).toLocalDateTime()
}
