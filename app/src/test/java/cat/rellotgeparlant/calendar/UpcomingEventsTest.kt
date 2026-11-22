package cat.rellotgeparlant.calendar

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime

class UpcomingEventsTest {
    private val now = LocalDateTime.of(2026, 11, 22, 10, 0)
    private fun at(h: Int, m: Int, allDay: Boolean = false) =
        CalendarEvent(now.withHour(h).withMinute(m), allDay, "E$h:$m", null, null)

    @Test fun windowBounds() {
        val r = UpcomingEvents.select(listOf(at(9, 59), at(10, 0), at(10, 1), at(16, 0), at(16, 1)), now, 6)
        assertEquals(listOf(at(10, 1), at(16, 0)), r)
    }

    @Test fun excludesAllDay() = assertTrue(UpcomingEvents.select(listOf(at(11, 0, allDay = true)), now, 6).isEmpty())

    @Test fun sorted() = assertEquals(
        listOf(at(11, 0), at(12, 0)), UpcomingEvents.select(listOf(at(12, 0), at(11, 0)), now, 6),
    )

    @Test fun emptyGivesEmpty() = assertTrue(UpcomingEvents.select(emptyList(), now, 6).isEmpty())
}
