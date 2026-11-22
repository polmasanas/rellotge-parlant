package cat.rellotgeparlant.service

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import cat.rellotgeparlant.calendar.EventSettings
import cat.rellotgeparlant.speech.AndroidTtsEngine
import java.time.LocalTime

object Prefs {
    private const val FILE = "rellotge"
    private const val KEY_RATE = "speech_rate"
    private const val KEY_ENABLED = "speech_enabled"
    private const val KEY_CALENDAR = "calendar_id"
    private const val KEY_HOURS = "hours_ahead"
    private const val KEY_REMINDER = "reminder_minutes"
    private const val KEY_MORNING = "morning_minute_of_day"
    private const val KEY_READ_LOCATION = "read_location"
    private const val KEY_READ_DESCRIPTION = "read_description"

    private fun p(context: Context): SharedPreferences = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun speechEnabled(context: Context): Boolean = p(context).getBoolean(KEY_ENABLED, true)
    fun setSpeechEnabled(context: Context, enabled: Boolean) = p(context).edit { putBoolean(KEY_ENABLED, enabled) }

    fun speechRate(context: Context): Float = p(context).getFloat(KEY_RATE, AndroidTtsEngine.DEFAULT_RATE)
    fun setSpeechRate(context: Context, rate: Float) = p(context).edit { putFloat(KEY_RATE, rate) }

    fun calendarId(context: Context): Long = p(context).getLong(KEY_CALENDAR, -1L)
    fun setCalendarId(context: Context, id: Long) = p(context).edit { putLong(KEY_CALENDAR, id) }

    fun eventSettings(context: Context): EventSettings {
        val d = EventSettings()
        val prefs = p(context)
        val morning = prefs.getInt(KEY_MORNING, d.morningTime.hour * 60 + d.morningTime.minute)
        return EventSettings(
            hoursAhead = prefs.getInt(KEY_HOURS, d.hoursAhead),
            reminderMinutes = prefs.getInt(KEY_REMINDER, d.reminderMinutes),
            morningTime = LocalTime.of(morning / 60, morning % 60),
            readLocation = prefs.getBoolean(KEY_READ_LOCATION, d.readLocation),
            readDescription = prefs.getBoolean(KEY_READ_DESCRIPTION, d.readDescription),
        )
    }

    fun setEventSettings(context: Context, s: EventSettings) = p(context).edit {
        putInt(KEY_HOURS, s.hoursAhead)
        putInt(KEY_REMINDER, s.reminderMinutes)
        putInt(KEY_MORNING, s.morningTime.hour * 60 + s.morningTime.minute)
        putBoolean(KEY_READ_LOCATION, s.readLocation)
        putBoolean(KEY_READ_DESCRIPTION, s.readDescription)
    }
}
