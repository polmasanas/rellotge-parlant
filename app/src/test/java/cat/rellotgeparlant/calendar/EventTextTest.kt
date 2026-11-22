package cat.rellotgeparlant.calendar

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

class EventTextTest {
    private val today = LocalDate.of(2026, 11, 22)
    private val now = today.atTime(15, 0)
    private val s = EventSettings()
    private fun ev(h: Int, m: Int, title: String? = "Metge", loc: String? = null, desc: String? = null, date: LocalDate = today) =
        CalendarEvent(LocalDateTime.of(date, java.time.LocalTime.of(h, m)), false, title, loc, desc)

    @Test fun describeFull() = assertEquals(
        "D'aquí dues hores i trenta minuts: Metge de capçalera. Lloc: CAP de Gràcia. Porta la targeta sanitària i les anàlisis.",
        EventText.describe(ev(17, 30, "Metge de capçalera", "CAP de Gràcia", "Porta la targeta sanitària i les anàlisis. https://x.y/z"), now, s),
    )

    @Test fun describeAfterMidnight() = assertTrue(
        EventText.describe(ev(0, 30, date = today.plusDays(1)), today.atTime(22, 0), s).startsWith("D'aquí dues hores i trenta minuts: "),
    )

    @Test fun describeNow() = assertEquals("Ara mateix: Metge.", EventText.describe(ev(15, 0), now, s))

    @Test fun describeRespectsSwitches() = assertEquals(
        "D'aquí dues hores: Metge.",
        EventText.describe(ev(17, 0, loc = "CAP", desc = "Detall."), now, s.copy(readLocation = false, readDescription = false)),
    )

    @Test fun emptyTitle() = assertTrue(EventText.describe(ev(17, 0, title = "  "), now, s).contains(": un esdeveniment."))

    @Test fun titleKeepsOwnFullStop() = assertEquals(
        "D'aquí dues hores: Dinar.", EventText.describe(ev(17, 0, title = "Dinar."), now, s),
    )

    @Test fun cleanStripsHtmlAndLinks() = assertEquals(
        "Hola iaia mira", EventText.cleanDescription("<p>Hola <b>iaia</b></p> www.x.com mira https://a.b"),
    )

    @Test fun cleanTwoSentencesMax() = assertEquals("U. Dos.", EventText.cleanDescription("U. Dos. Tres."))

    @Test fun cleanMax200() {
        val c = EventText.cleanDescription("a".repeat(500))!!
        assertTrue(c.length <= 200)
        assertTrue(c.endsWith("…"))
    }

    @Test fun cleanBlankIsNull() = assertNull(EventText.cleanDescription("  <br> "))

    @Test fun upcomingLimitsToThree() {
        val r = EventText.upcomingSentence((1..5).map { ev(15 + it, 0, "E$it") }, now, s)!!
        assertTrue(r, r.endsWith("I 2 més."))
        assertTrue(r.contains("E3") && !r.contains("E4"))
    }

    @Test fun upcomingEmptyIsNull() = assertNull(EventText.upcomingSentence(emptyList(), now, s))

    @Test fun morningSentence() = assertEquals(
        "Bon dia. Avui: Aniversari de la Maria, Festa.",
        EventText.morningSentence(listOf(
            CalendarEvent(today.atStartOfDay(), true, "Aniversari de la Maria", null, null),
            CalendarEvent(today.atStartOfDay(), true, "Festa.", null, null),
        )),
    )

    @Test fun morningEmptyIsNull() = assertNull(EventText.morningSentence(emptyList()))
}
