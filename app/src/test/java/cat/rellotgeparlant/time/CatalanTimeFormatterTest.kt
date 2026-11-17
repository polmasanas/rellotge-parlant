package cat.rellotgeparlant.time

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Test

class CatalanTimeFormatterTest {

    private fun p(h: Int, m: Int) = CatalanTimeFormatter.phrase(h, m)

    @Test fun goldenExamples() {
        assertEquals("Són dos quarts i cinc de cinc de la tarda.", p(16, 35))
        assertEquals("Són tres quarts d'una del migdia.", p(12, 45))
        assertEquals("Són dos quarts d'onze del matí.", p(10, 30))
        assertEquals("És la una del migdia.", p(13, 0))
        assertEquals("Són les dotze de la nit.", p(0, 0))
        assertEquals("Són les vuit menys cinc del matí.", p(7, 55))
        assertEquals("És un quart de deu del vespre.", p(21, 15))
    }

    @Test fun everyAnchorForFourPm() {
        val t = " de la tarda."
        assertEquals("Són les quatre$t", p(16, 0))
        assertEquals("Són les quatre i cinc$t", p(16, 5))
        assertEquals("És mig quart de cinc$t", p(16, 8))
        assertEquals("És un quart menys cinc de cinc$t", p(16, 10))
        assertEquals("És un quart de cinc$t", p(16, 15))
        assertEquals("És un quart i cinc de cinc$t", p(16, 20))
        assertEquals("És un quart i mig de cinc$t", p(16, 23))
        assertEquals("Són dos quarts menys cinc de cinc$t", p(16, 25))
        assertEquals("Són dos quarts de cinc$t", p(16, 30))
        assertEquals("Són dos quarts i cinc de cinc$t", p(16, 35))
        assertEquals("Són dos quarts i mig de cinc$t", p(16, 38))
        assertEquals("Són tres quarts menys cinc de cinc$t", p(16, 40))
        assertEquals("Són tres quarts de cinc$t", p(16, 45))
        assertEquals("Són tres quarts i cinc de cinc$t", p(16, 50))
        assertEquals("Són tres quarts i mig de cinc$t", p(16, 53))
        assertEquals("Són les cinc menys cinc$t", p(16, 55))
    }

    @Test fun roundsDown() {
        assertEquals(p(16, 30), p(16, 33))
        assertEquals(p(16, 35), p(16, 37))
        assertEquals(p(16, 5), p(16, 7))
        assertEquals(p(16, 8), p(16, 9))
        assertEquals("Són les dotze menys cinc del matí.", p(11, 58))
    }

    @Test fun wrapAround() {
        assertEquals("Són dos quarts d'una del migdia.", p(12, 30))
        assertEquals("Són les dotze menys cinc de la nit.", p(23, 58))
        assertEquals("Són les dotze i cinc de la nit.", p(0, 7))
        assertEquals("Són dos quarts d'una de la nit.", p(0, 30))
        assertEquals("Són les dotze del migdia.", p(12, 0))
    }

    @Test fun singularOne() {
        assertEquals("És la una de la matinada.", p(1, 0))
        assertEquals("És la una menys cinc de la nit.", p(0, 55))
        assertEquals("És la una i cinc del migdia.", p(13, 5))
    }

    @Test fun elision() {
        assertEquals("És un quart d'onze del matí.", p(10, 15))
        assertEquals("És un quart de dues de la matinada.", p(1, 15))
    }

    @Test fun dayPartBands() {
        assertEquals("Són les dotze de la nit.", p(0, 0))
        assertEquals("És la una de la matinada.", p(1, 0))
        assertEquals("Són les cinc de la matinada.", p(5, 0))
        assertEquals("Són les sis del matí.", p(6, 0))
        assertEquals("Són les onze del matí.", p(11, 0))
        assertEquals("Són les dotze del migdia.", p(12, 0))
        assertEquals("Són les dues del migdia.", p(14, 0))
        assertEquals("Són les tres de la tarda.", p(15, 0))
        assertEquals("Són les set de la tarda.", p(19, 0))
        assertEquals("Són les vuit del vespre.", p(20, 0))
        assertEquals("Són les nou del vespre.", p(21, 0))
        assertEquals("Són les deu de la nit.", p(22, 0))
        assertEquals("Són les onze de la nit.", p(23, 0))
    }

    @Test fun allMinutesProduceSentence() {
        for (h in 0..23) for (m in 0..59) {
            val s = p(h, m)
            assertTrue("$h:$m → $s", s.startsWith("És ") || s.startsWith("Són "))
            assertTrue("$h:$m → $s", s.endsWith("."))
            assertFalse("$h:$m → $s", s.contains("de u") || s.contains("de o"))
            assertFalse("$h:$m → $s", s.contains("  "))
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsOutOfRangeHour() { p(24, 0) }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsOutOfRangeMinute() { p(10, 60) }
}
