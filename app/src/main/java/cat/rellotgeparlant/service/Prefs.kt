package cat.rellotgeparlant.service

import android.content.Context
import androidx.core.content.edit
import cat.rellotgeparlant.speech.AndroidTtsEngine

object Prefs {
    private const val FILE = "rellotge"
    private const val KEY_RATE = "speech_rate"
    private const val KEY_ENABLED = "speech_enabled"

    fun speechEnabled(context: Context): Boolean =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).getBoolean(KEY_ENABLED, true)

    fun setSpeechEnabled(context: Context, enabled: Boolean) =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit { putBoolean(KEY_ENABLED, enabled) }

    fun speechRate(context: Context): Float =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).getFloat(KEY_RATE, AndroidTtsEngine.DEFAULT_RATE)

    fun setSpeechRate(context: Context, rate: Float) =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit { putFloat(KEY_RATE, rate) }
}
