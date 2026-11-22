package cat.rellotgeparlant.service

import cat.rellotgeparlant.speech.Speaker
import cat.rellotgeparlant.time.CatalanTimeFormatter
import java.time.Clock
import java.time.LocalTime

/** Turns a screen event into a spoken time, reading the clock at the moment of speaking. */
class TimeAnnouncer(
    private val clock: Clock,
    private val speaker: Speaker,
    private val events: () -> String? = { null },
) {

    fun onScreenEvent(event: ScreenEvent, enabled: Boolean, keyguardLocked: Boolean, audioMode: Int) {
        if (!SpeakPolicy.shouldSpeak(enabled, event, keyguardLocked, audioMode)) return
        speakNow()
    }

    fun speakNow() {
        val now = LocalTime.now(clock)
        val time = CatalanTimeFormatter.phrase(now.hour, now.minute)
        // Calendar trouble must never cost her the time.
        val upcoming = runCatching { events() }.getOrNull()
        speaker.say(upcoming?.let { "$time $it" } ?: time)
    }
}

/** System clock that follows timezone/DST changes made while the service is running. */
object LiveSystemClock : Clock() {
    override fun getZone(): java.time.ZoneId = java.time.ZoneId.systemDefault()
    override fun withZone(zone: java.time.ZoneId): Clock = Clock.system(zone)
    override fun instant(): java.time.Instant = java.time.Instant.now()
}
