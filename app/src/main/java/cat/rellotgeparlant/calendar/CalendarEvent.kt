package cat.rellotgeparlant.calendar

import java.time.LocalDateTime
import java.time.LocalTime

/** One occurrence of a calendar event. For all-day events [start] is the local date at 00:00. */
data class CalendarEvent(
    val start: LocalDateTime,
    val allDay: Boolean,
    val title: String?,
    val location: String?,
    val description: String?,
)

data class EventSettings(
    val hoursAhead: Int = 6,
    val reminderMinutes: Int = 60,
    val morningTime: LocalTime = LocalTime.of(9, 0),
    val readLocation: Boolean = true,
    val readDescription: Boolean = true,
)
