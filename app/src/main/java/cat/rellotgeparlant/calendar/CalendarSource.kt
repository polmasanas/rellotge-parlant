package cat.rellotgeparlant.calendar

import android.content.ContentUris
import android.content.Context
import android.provider.CalendarContract
import java.time.LocalDateTime
import java.time.ZoneId

/** Reads the phone's synced calendars. Returns nothing (never throws) without permission or on provider errors. */
class CalendarSource(context: Context) {
    private val resolver = context.applicationContext.contentResolver

    fun calendars(): List<Pair<Long, String>> = try {
        resolver.query(
            CalendarContract.Calendars.CONTENT_URI,
            arrayOf(CalendarContract.Calendars._ID, CalendarContract.Calendars.CALENDAR_DISPLAY_NAME),
            null, null, CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
        )?.use { c ->
            buildList { while (c.moveToNext()) add(c.getLong(0) to (c.getString(1) ?: "?")) }
        } ?: emptyList()
    } catch (_: Exception) {
        emptyList()
    }

    fun events(calendarId: Long, from: LocalDateTime, to: LocalDateTime): List<CalendarEvent> {
        if (calendarId < 0) return emptyList()
        val zone = ZoneId.systemDefault()
        val uri = CalendarContract.Instances.CONTENT_URI.buildUpon().also {
            ContentUris.appendId(it, from.atZone(zone).toInstant().toEpochMilli())
            ContentUris.appendId(it, to.atZone(zone).toInstant().toEpochMilli())
        }.build()
        return try {
            resolver.query(
                uri,
                arrayOf(
                    CalendarContract.Instances.BEGIN,
                    CalendarContract.Instances.ALL_DAY,
                    CalendarContract.Instances.TITLE,
                    CalendarContract.Instances.EVENT_LOCATION,
                    CalendarContract.Instances.DESCRIPTION,
                ),
                "${CalendarContract.Instances.CALENDAR_ID} = ?",
                arrayOf(calendarId.toString()),
                CalendarContract.Instances.BEGIN,
            )?.use { c ->
                buildList {
                    while (c.moveToNext()) {
                        val allDay = c.getInt(1) == 1
                        val start = if (allDay) CalendarDates.allDayDate(c.getLong(0)).atStartOfDay()
                        else CalendarDates.timedStart(c.getLong(0), zone)
                        add(CalendarEvent(start, allDay, c.getString(2), c.getString(3), c.getString(4)))
                    }
                }
            } ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }
}
