package cat.rellotgeparlant.calendar

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.TimeZone

class CalendarDatesTest {
    @Test fun allDayDateUsesUtc() {
        val saved = TimeZone.getDefault()
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("America/Los_Angeles"))
            assertEquals(LocalDate.of(2025, 11, 25), CalendarDates.allDayDate(1764028800000))
        } finally {
            TimeZone.setDefault(saved)
        }
    }

    @Test fun timedStartUsesGivenZone() = assertEquals(
        LocalDateTime.of(2025, 11, 25, 1, 0),
        CalendarDates.timedStart(1764028800000, ZoneId.of("Europe/Madrid")),
    )
}
