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
import android.database.ContentObserver
import android.os.Handler
import android.provider.CalendarContract
import android.media.AudioManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import cat.rellotgeparlant.R
import cat.rellotgeparlant.calendar.CalendarSource
import cat.rellotgeparlant.calendar.EventText
import cat.rellotgeparlant.calendar.ReminderScheduler
import cat.rellotgeparlant.calendar.UpcomingEvents
import java.time.LocalDateTime
import cat.rellotgeparlant.speech.AndroidTtsEngine
import cat.rellotgeparlant.speech.Speaker
import cat.rellotgeparlant.ui.MainActivity

/** Always-on service: speaks the time when the locked phone's screen turns on, and when the screen turns off. */
class SpeakerService : Service() {

    private lateinit var speaker: Speaker
    private lateinit var announcer: TimeAnnouncer

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val event = when (intent.action) {
                Intent.ACTION_SCREEN_ON -> ScreenEvent.ON
                Intent.ACTION_SCREEN_OFF -> ScreenEvent.OFF
                else -> return
            }
            val keyguard = getSystemService(KeyguardManager::class.java)
            val audio = getSystemService(AudioManager::class.java)
            announcer.onScreenEvent(event, Prefs.speechEnabled(context), keyguard.isKeyguardLocked, audio.mode)
        }
    }

    // Calendar syncs send bursts of change notifications: reschedule once, off the main thread.
    private val calendarThread = android.os.HandlerThread("calendar").apply { start() }
    private val calendarHandler = Handler(calendarThread.looper)
    private val rescheduleTask = Runnable { ReminderScheduler.reschedule(this) }

    private val calendarObserver = object : ContentObserver(calendarHandler) {
        override fun onChange(selfChange: Boolean) {
            calendarHandler.removeCallbacks(rescheduleTask)
            calendarHandler.postDelayed(rescheduleTask, RESCHEDULE_DEBOUNCE_MS)
        }
    }

    /** Upcoming events from the chosen calendar, as one spoken sentence, or null. */
    private fun upcomingEvents(): String? {
        val now = LocalDateTime.now()
        val s = Prefs.eventSettings(this)
        val events = CalendarSource(this).events(Prefs.calendarId(this), now, now.plusHours(s.hoursAhead.toLong()))
        return EventText.upcomingSentence(UpcomingEvents.select(events, now, s.hoursAhead), now.toLocalDate(), s)
    }

    override fun onCreate() {
        super.onCreate()
        isRunning = true
        speaker = Speaker(AndroidTtsEngine(this))
        speaker.setRate(Prefs.speechRate(this))
        speaker.setVolume(Prefs.voiceVolume(this))
        announcer = TimeAnnouncer(LiveSystemClock, speaker) { upcomingEvents() }
        runCatching {
            contentResolver.registerContentObserver(CalendarContract.Events.CONTENT_URI, true, calendarObserver)
        }
        ReminderScheduler.reschedule(this)
        ContextCompat.registerReceiver(
            this, screenReceiver,
            IntentFilter().apply {
                addAction(Intent.ACTION_SCREEN_ON)
                addAction(Intent.ACTION_SCREEN_OFF)
            },
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
            ACTION_VOLUME_CHANGED -> speaker.setVolume(Prefs.voiceVolume(this))
            ACTION_SPEAK_EVENTS_TEST -> speaker.say(upcomingEvents() ?: getString(R.string.no_upcoming_events))
            ACTION_SPEAK_TEXT -> {
                val text = intent.getStringExtra(EXTRA_TEXT)
                val audio = getSystemService(AudioManager::class.java)
                if (text != null && Prefs.speechEnabled(this) && audio.mode == AudioManager.MODE_NORMAL) {
                    speaker.say(text)
                }
                ReminderScheduler.reschedule(this)
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        unregisterReceiver(screenReceiver)
        contentResolver.unregisterContentObserver(calendarObserver)
        calendarThread.quitSafely()
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
        const val ACTION_VOLUME_CHANGED = "cat.rellotgeparlant.VOLUME_CHANGED"
        const val ACTION_SPEAK_TEXT = "cat.rellotgeparlant.SPEAK_TEXT"
        const val ACTION_SPEAK_EVENTS_TEST = "cat.rellotgeparlant.SPEAK_EVENTS_TEST"
        const val EXTRA_TEXT = "text"
        private const val RESCHEDULE_DEBOUNCE_MS = 2_000L

        /**
         * Hands an action to the service. If it's already running (a foreground service), a plain
         * startService is allowed from the background; only start it as foreground when it isn't.
         */
        fun deliver(context: Context, action: String, text: String? = null) {
            if (isRunning) {
                context.startService(Intent(context, SpeakerService::class.java).setAction(action).putExtra(EXTRA_TEXT, text))
            } else {
                ensureRunning(context, action, text)
            }
        }

        @Volatile var isRunning = false
            private set

        fun ensureRunning(context: Context, action: String? = null, text: String? = null) {
            val intent = Intent(context, SpeakerService::class.java).setAction(action).putExtra(EXTRA_TEXT, text)
            ContextCompat.startForegroundService(context, intent)
        }
    }
}
