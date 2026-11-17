package cat.rellotgeparlant.service

import cat.rellotgeparlant.speech.Speaker
import cat.rellotgeparlant.time.CatalanTimeFormatter
import java.time.Clock
import java.time.LocalTime

/** Turns a screen wake into a spoken time, reading the clock at the moment of speaking. */
class TimeAnnouncer(private val clock: Clock, private val speaker: Speaker) {

    fun onScreenOn(keyguardLocked: Boolean, audioMode: Int) {
        if (!SpeakPolicy.shouldSpeak(keyguardLocked, audioMode)) return
        speakNow()
    }

    fun speakNow() {
        val now = LocalTime.now(clock)
        speaker.say(CatalanTimeFormatter.phrase(now.hour, now.minute))
    }
}

/** System clock that follows timezone/DST changes made while the service is running. */
object LiveSystemClock : Clock() {
    override fun getZone(): java.time.ZoneId = java.time.ZoneId.systemDefault()
    override fun withZone(zone: java.time.ZoneId): Clock = Clock.system(zone)
    override fun instant(): java.time.Instant = java.time.Instant.now()
}
