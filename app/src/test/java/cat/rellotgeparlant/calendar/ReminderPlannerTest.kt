package cat.rellotgeparlant.calendar

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

class ReminderPlannerTest {
    private val day = LocalDate.of(2026, 11, 22)
    private val now = day.atTime(10, 0)
    private val s = EventSettings()
    private fun ev(h: Int, m: Int = 0, title: String = "Metge") = CalendarEvent(day.atTime(h, m), false, title, null, null)
    private fun allDay(date: LocalDate, title: String = "Aniversari") = CalendarEvent(date.atStartOfDay(), true, title, null, null)

    @Test fun reminderOneHourBefore() = assertEquals(
        PlannedSpeech(day.atTime(11, 0), "Recordatori: d'aquí una hora: Metge."),
        ReminderPlanner.next(listOf(ev(12)), now, s),
    )

    @Test fun skipsPastReminder() =
        assertEquals(day.atTime(13, 0), ReminderPlanner.next(listOf(ev(10, 30), ev(14)), now, s)!!.at)

    @Test fun twoInstancesTwoReminders() {
        val events = listOf(ev(12), ev(15))
        assertEquals(day.atTime(11, 0), ReminderPlanner.next(events, now, s)!!.at)
        assertEquals(day.atTime(14, 0), ReminderPlanner.next(events, day.atTime(11, 0, 1), s)!!.at)
    }

    @Test fun reminderOffMeansOnlyMorning() =
        assertNull(ReminderPlanner.next(listOf(ev(12)), now, s.copy(reminderMinutes = 0)))

    @Test fun morningToday() = assertEquals(
        PlannedSpeech(day.atTime(9, 0), "Bon dia. Avui: Aniversari."),
        ReminderPlanner.next(listOf(allDay(day)), day.atTime(8, 0), s),
    )

    @Test fun morningTomorrowWhenPassed() = assertEquals(
        day.plusDays(1).atTime(9, 0), ReminderPlanner.next(listOf(allDay(day.plusDays(1))), now, s)!!.at,
    )

    @Test fun noMorningWithoutAllDay() =
        assertNull(ReminderPlanner.next(listOf(allDay(day.plusDays(3))), now, s))

    @Test fun earliestWins() = assertEquals(
        day.atTime(13, 0), ReminderPlanner.next(listOf(allDay(day.plusDays(1)), ev(14)), now, s)!!.at,
    )

    @Test fun emptyIsNull() = assertNull(ReminderPlanner.next(emptyList(), now, s))

    @Test fun morningAndReminderAtSameTimeAreBothSaid() {
        // default morning 09:00 + reminder 60 min for a 10:00 event → both due at 09:00
        val r = ReminderPlanner.next(listOf(allDay(day, "Aniversari de la Núria"), ev(10, title = "Metge")), day.atTime(8, 0), s)!!
        assertEquals(day.atTime(9, 0), r.at)
        assertEquals("Bon dia. Avui: Aniversari de la Núria. Recordatori: d'aquí una hora: Metge.", r.text)
    }

    @Test fun twoEventsSameStartBothReminded() {
        val r = ReminderPlanner.next(listOf(ev(12, title = "Metge"), ev(12, title = "Farmàcia")), now, s)!!
        assertEquals("Recordatori: d'aquí una hora: Metge. Recordatori: d'aquí una hora: Farmàcia.", r.text)
    }
}
