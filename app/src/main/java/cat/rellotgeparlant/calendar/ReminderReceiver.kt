package cat.rellotgeparlant.calendar

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import cat.rellotgeparlant.service.SpeakerService

/** Fired by the alarm: speaks the planned text through the service (which then reschedules). */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val text = intent.getStringExtra(EXTRA_TEXT)
        if (text == null) {
            ReminderScheduler.reschedule(context)
            return
        }
        runCatching { SpeakerService.ensureRunning(context, SpeakerService.ACTION_SPEAK_TEXT, text) }
            .onFailure { ReminderScheduler.reschedule(context) }
    }

    companion object {
        const val EXTRA_TEXT = "text"
    }
}
