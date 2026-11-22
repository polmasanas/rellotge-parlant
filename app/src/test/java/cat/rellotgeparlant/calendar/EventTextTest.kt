package cat.rellotgeparlant.calendar

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

class EventTextTest {
    private val today = LocalDate.of(2026, 11, 22)
    private val s = EventSettings()
    private fun ev(h: Int, m: Int, title: String? = "Metge", loc: String? = null, desc: String? = null, date: LocalDate = today) =
        CalendarEvent(LocalDateTime.of(date, java.time.LocalTime.of(h, m)), false, title, loc, desc)

    @Test fun describeFull() = assertEquals(
        "Avui, a dos quarts de sis de la tarda: Metge de capçalera. Lloc: CAP de Gràcia. Porta la targeta sanitària i les anàlisis.",
        EventText.describe(ev(17, 30, "Metge de capçalera", "CAP de Gràcia", "Porta la targeta sanitària i les anàlisis. https://x.y/z"), today, s),
    )

    @Test fun describeTomorrow() = assertTrue(
        EventText.describe(ev(0, 30, date = today.plusDays(1)), today, s).startsWith("Demà, a dos quarts d'una de la nit: "),
    )

    @Test fun describeRespectsSwitches() = assertEquals(
        "Avui, a les cinc de la tarda: Metge.",
        EventText.describe(ev(17, 0, loc = "CAP", desc = "Detall."), today, s.copy(readLocation = false, readDescription = false)),
    )

    @Test fun emptyTitle() = assertTrue(EventText.describe(ev(17, 0, title = "  "), today, s).contains(": un esdeveniment."))

    @Test fun titleKeepsOwnFullStop() = assertEquals(
        "Avui, a les cinc de la tarda: Dinar.", EventText.describe(ev(17, 0, title = "Dinar."), today, s),
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
        val r = EventText.upcomingSentence((1..5).map { ev(12 + it, 0, "E$it") }, today, s)!!
        assertTrue(r, r.endsWith("I 2 més."))
        assertTrue(r.contains("E3") && !r.contains("E4"))
    }

    @Test fun upcomingEmptyIsNull() = assertNull(EventText.upcomingSentence(emptyList(), today, s))

    @Test fun morningSentence() = assertEquals(
        "Bon dia. Avui: Aniversari de la Maria, Festa.",
        EventText.morningSentence(listOf(
            CalendarEvent(today.atStartOfDay(), true, "Aniversari de la Maria", null, null),
            CalendarEvent(today.atStartOfDay(), true, "Festa.", null, null),
        )),
    )

    @Test fun morningEmptyIsNull() = assertNull(EventText.morningSentence(emptyList()))
}
