package cat.rellotgeparlant.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Restarts the service after a reboot or an app update. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            SpeakerService.ensureRunning(context)
            WatchdogWorker.schedule(context)
            cat.rellotgeparlant.calendar.ReminderScheduler.reschedule(context)
        }
    }
}
