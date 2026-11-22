package cat.rellotgeparlant.time

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RelativeTimeTest {
    private fun r(min: Long) = RelativeTime.phrase(min)

    @Test fun hoursAndMinutes() = assertEquals("d'aquí dues hores i quinze minuts", r(135))
    @Test fun oneHourOneMinute() = assertEquals("d'aquí una hora i un minut", r(61))
    @Test fun exactHours() = assertEquals("d'aquí una hora", r(60))
    @Test fun feminineHours() = assertEquals("d'aquí vint-i-dues hores", r(22 * 60))
    @Test fun minutesOnly() = assertEquals("d'aquí quaranta minuts", r(40))
    @Test fun compoundMinutes() = assertEquals("d'aquí trenta-un minuts", r(31))
    @Test fun twentyOneMinutes() = assertEquals("d'aquí vint-i-un minuts", r(21))
    @Test fun oneMinute() = assertEquals("d'aquí un minut", r(1))
    @Test fun now() = assertEquals("ara mateix", r(0))
    @Test fun pastIsNow() = assertEquals("ara mateix", r(-5))

    @Test fun everyValueUpToADayIsWords() {
        for (m in 1L..24 * 60) {
            val s = r(m)
            assertTrue("$m → $s", s.startsWith("d'aquí ") && s.none { it.isDigit() } && !s.contains("  "))
        }
    }
}
