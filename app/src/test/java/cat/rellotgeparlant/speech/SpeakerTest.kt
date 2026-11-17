package cat.rellotgeparlant.speech

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SpeakerTest {

    @Test fun startsInitialising() {
        assertEquals(VoiceStatus.INITIALISING, Speaker(FakeTtsEngine()).status)
    }

    @Test fun pendingTextSpokenOnceWhenReady() {
        val e = FakeTtsEngine()
        val s = Speaker(e)
        s.say("primer")
        s.say("darrer")
        assertTrue(e.spoken.isEmpty())
        e.ready()
        assertEquals(listOf("darrer" to true), e.spoken)
        assertEquals(VoiceStatus.READY, s.status)
    }

    @Test fun speaksWithFlushWhenReady() {
        val e = FakeTtsEngine()
        val s = Speaker(e).also { e.ready() }
        s.say("hola")
        assertEquals(listOf("hola" to true), e.spoken)
    }

    @Test fun missingCatalanStaysSilent() {
        val e = FakeTtsEngine(catalan = false)
        val s = Speaker(e)
        s.say("pendent")
        e.ready()
        s.say("hola")
        assertEquals(VoiceStatus.MISSING, s.status)
        assertTrue(e.spoken.isEmpty())
    }

    @Test fun initFailureIsMissing() {
        val e = FakeTtsEngine()
        val s = Speaker(e)
        e.ready(false)
        s.say("hola")
        assertEquals(VoiceStatus.MISSING, s.status)
        assertTrue(e.spoken.isEmpty())
    }

    @Test fun rateIsForwarded() {
        val e = FakeTtsEngine()
        Speaker(e).setRate(0.85f)
        assertEquals(0.85f, e.rate)
    }
}
