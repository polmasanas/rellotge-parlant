package cat.rellotgeparlant.setup

import android.Manifest
import android.app.KeyguardManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.speech.tts.TextToSpeech
import androidx.core.content.ContextCompat
import cat.rellotgeparlant.service.SpeakerService
import cat.rellotgeparlant.speech.AndroidTtsEngine
import cat.rellotgeparlant.speech.VoiceStatus

enum class CheckId { NOTIFICATIONS, BATTERY_OPTIMISATION, CATALAN_VOICE, SERVICE_RUNNING, SWIPE_LOCK, NOTIFICATION_WAKE, OEM_AUTOSTART }

/** ok == null means it can't be detected automatically: shown as an instruction. */
data class Check(val id: CheckId, val ok: Boolean?, val fix: Intent?)

fun runChecks(context: Context, voiceStatus: VoiceStatus): List<Check> {
    val pkg = context.packageName
    val appUri = Uri.parse("package:$pkg")
    val checks = mutableListOf<Check>()

    val notificationsOk = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    checks += Check(
        CheckId.NOTIFICATIONS, notificationsOk,
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, pkg),
    )

    val power = context.getSystemService(PowerManager::class.java)
    checks += Check(
        CheckId.BATTERY_OPTIMISATION, power.isIgnoringBatteryOptimizations(pkg),
        @Suppress("BatteryLife")
        Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, appUri),
    )

    val voiceOk = when (voiceStatus) {
        VoiceStatus.READY -> true
        VoiceStatus.MISSING -> false
        VoiceStatus.INITIALISING -> null
    }
    // Always target Google's engine: Samsung's own engine has no Catalan voice.
    val voiceFix = if (AndroidTtsEngine.isGoogleEngineInstalled(context)) {
        Intent(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA).setPackage(AndroidTtsEngine.GOOGLE_TTS)
    } else {
        Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=${AndroidTtsEngine.GOOGLE_TTS}"))
    }
    checks += Check(CheckId.CATALAN_VOICE, voiceOk, voiceFix)

    checks += Check(CheckId.SERVICE_RUNNING, SpeakerService.isRunning, null)

    val keyguard = context.getSystemService(KeyguardManager::class.java)
    checks += Check(
        CheckId.SWIPE_LOCK, if (keyguard.isKeyguardSecure) false else null,
        Intent(Settings.ACTION_SECURITY_SETTINGS),
    )

    checks += Check(CheckId.NOTIFICATION_WAKE, null, Intent(Settings.ACTION_DISPLAY_SETTINGS))

    OemAutostart.targetFor(Build.MANUFACTURER)?.let { t ->
        checks += Check(
            CheckId.OEM_AUTOSTART, null,
            Intent().setComponent(ComponentName(t.packageName, t.className)),
        )
    }
    return checks
}
