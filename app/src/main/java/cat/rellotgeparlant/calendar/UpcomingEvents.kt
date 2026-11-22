package cat.rellotgeparlant.calendar

import java.time.LocalDateTime

object UpcomingEvents {
    /** Timed events starting after [now] and at most [hoursAhead] hours later, earliest first. */
    fun select(events: List<CalendarEvent>, now: LocalDateTime, hoursAhead: Int): List<CalendarEvent> {
        val end = now.plusHours(hoursAhead.toLong())
        return events.filter { !it.allDay && it.start.isAfter(now) && !it.start.isAfter(end) }
            .sortedBy { it.start }
    }
}
