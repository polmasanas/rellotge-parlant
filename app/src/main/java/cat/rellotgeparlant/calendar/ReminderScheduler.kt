package cat.rellotgeparlant.calendar

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import cat.rellotgeparlant.service.Prefs
import java.time.LocalDateTime
import java.time.ZoneId

/** Keeps exactly one alarm set: the next reminder or morning announcement (or a re-check). */
object ReminderScheduler {
    private const val LOOKAHEAD_HOURS = 48L
    private const val RECHECK_HOURS = 12L

    fun reschedule(context: Context) {
        val now = LocalDateTime.now()
        val settings = Prefs.eventSettings(context)
        val events = CalendarSource(context).events(Prefs.calendarId(context), now, now.plusHours(LOOKAHEAD_HOURS))
        val plan = ReminderPlanner.next(events, now, settings)
        // With nothing to say, still wake up later to re-check (the calendar observer may not be running).
        val at = plan?.at ?: now.plusHours(RECHECK_HOURS)
        val millis = at.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

        val intent = Intent(context, ReminderReceiver::class.java).putExtra(ReminderReceiver.EXTRA_TEXT, plan?.text)
        val pending = PendingIntent.getBroadcast(
            context, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val alarms = context.getSystemService(AlarmManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarms.canScheduleExactAlarms()) {
            alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, pending)
        } else {
            alarms.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, pending)
        }
    }
}
