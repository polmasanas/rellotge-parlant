package cat.rellotgeparlant.service

import cat.rellotgeparlant.speech.FakeTtsEngine
import cat.rellotgeparlant.speech.Speaker
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

class TimeAnnouncerTest {

    /** A Clock whose instant and zone can be changed between calls. */
    private class MutableClock(var instant: Instant, var zoneId: ZoneId) : Clock() {
        override fun getZone() = zoneId
        override fun withZone(zone: ZoneId) = MutableClock(instant, zone)
        override fun instant() = instant
    }

    private val engine = FakeTtsEngine().also { }
    private val speaker = Speaker(engine).also { engine.ready() }
    private val clock = MutableClock(Instant.parse("2026-11-17T16:35:00Z"), ZoneOffset.UTC)
    private val announcer = TimeAnnouncer(clock, speaker)

    @Test fun lockedAndIdleSpeaksTime() {
        announcer.onScreenOn(keyguardLocked = true, audioMode = 0)
        assertEquals(listOf("Són dos quarts i cinc de cinc de la tarda."), engine.spoken.map { it.first })
    }

    @Test fun unlockedIsSilent() {
        announcer.onScreenOn(keyguardLocked = false, audioMode = 0)
        assertTrue(engine.spoken.isEmpty())
    }

    @Test fun inCallIsSilent() {
        announcer.onScreenOn(keyguardLocked = true, audioMode = 2)
        assertTrue(engine.spoken.isEmpty())
    }

    @Test fun readsClockOnEveryCall() {
        announcer.onScreenOn(true, 0)
        clock.instant = Instant.parse("2026-11-17T08:00:00Z")
        clock.zoneId = ZoneId.of("Europe/Madrid") // 09:00 local
        announcer.onScreenOn(true, 0)
        assertEquals("Són les nou del matí.", engine.spoken.last().first)
    }
}
