package cat.rellotgeparlant.service

import android.content.Context
import androidx.core.content.edit
import cat.rellotgeparlant.speech.AndroidTtsEngine

object Prefs {
    private const val FILE = "rellotge"
    private const val KEY_RATE = "speech_rate"

    fun speechRate(context: Context): Float =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).getFloat(KEY_RATE, AndroidTtsEngine.DEFAULT_RATE)

    fun setSpeechRate(context: Context, rate: Float) =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit { putFloat(KEY_RATE, rate) }
}
