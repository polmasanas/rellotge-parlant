package cat.rellotgeparlant.service

import android.app.KeyguardManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.media.AudioManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import cat.rellotgeparlant.R
import cat.rellotgeparlant.speech.AndroidTtsEngine
import cat.rellotgeparlant.speech.Speaker
import cat.rellotgeparlant.ui.MainActivity

/** Always-on service: speaks the time each time the locked phone's screen turns on. */
class SpeakerService : Service() {

    private lateinit var speaker: Speaker
    private lateinit var announcer: TimeAnnouncer

    private val screenOnReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action != Intent.ACTION_SCREEN_ON) return
            val keyguard = getSystemService(KeyguardManager::class.java)
            val audio = getSystemService(AudioManager::class.java)
            announcer.onScreenOn(keyguard.isKeyguardLocked, audio.mode)
        }
    }

    override fun onCreate() {
        super.onCreate()
        isRunning = true
        speaker = Speaker(AndroidTtsEngine(this))
        speaker.setRate(Prefs.speechRate(this))
        announcer = TimeAnnouncer(LiveSystemClock, speaker)
        ContextCompat.registerReceiver(
            this, screenOnReceiver, IntentFilter(Intent.ACTION_SCREEN_ON),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        } else 0
        ServiceCompat.startForeground(this, NOTIFICATION_ID, buildNotification(), type)
        when (intent?.action) {
            ACTION_SPEAK_NOW -> announcer.speakNow()
            ACTION_RATE_CHANGED -> speaker.setRate(Prefs.speechRate(this))
        }
        return START_STICKY
    }

    override fun onDestroy() {
        unregisterReceiver(screenOnReceiver)
        speaker.shutdown()
        isRunning = false
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun buildNotification(): Notification {
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, getString(R.string.channel_name), NotificationManager.IMPORTANCE_MIN)
        )
        val open = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(getString(R.string.notification_title))
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setContentIntent(open)
            .build()
    }

    companion object {
        private const val CHANNEL_ID = "rellotge"
        private const val NOTIFICATION_ID = 1
        const val ACTION_SPEAK_NOW = "cat.rellotgeparlant.SPEAK_NOW"
        const val ACTION_RATE_CHANGED = "cat.rellotgeparlant.RATE_CHANGED"

        @Volatile var isRunning = false
            private set

        fun ensureRunning(context: Context, action: String? = null) {
            val intent = Intent(context, SpeakerService::class.java).setAction(action)
            ContextCompat.startForegroundService(context, intent)
        }
    }
}
