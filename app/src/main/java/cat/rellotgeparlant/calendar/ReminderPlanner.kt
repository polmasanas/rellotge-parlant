package cat.rellotgeparlant.calendar

import java.time.LocalDateTime

data class PlannedSpeech(val at: LocalDateTime, val text: String)

/** Decides the next moment the phone should speak on its own, and what it says. */
object ReminderPlanner {
    fun next(events: List<CalendarEvent>, now: LocalDateTime, s: EventSettings): PlannedSpeech? {
        val candidates = mutableListOf<PlannedSpeech>()

        if (s.reminderMinutes > 0) {
            events.filter { !it.allDay }.forEach { e ->
                val at = e.start.minusMinutes(s.reminderMinutes.toLong())
                if (at.isAfter(now)) {
                    candidates += PlannedSpeech(at, "Recordatori: " + EventText.describe(e, at.toLocalDate(), s))
                }
            }
        }

        val todayMorning = now.toLocalDate().atTime(s.morningTime)
        val morning = if (todayMorning.isAfter(now)) todayMorning else todayMorning.plusDays(1)
        val allDayThatMorning = events.filter { it.allDay && it.start.toLocalDate() == morning.toLocalDate() }
        EventText.morningSentence(allDayThatMorning)?.let { candidates += PlannedSpeech(morning, it) }

        return candidates.minByOrNull { it.at }
    }
}
