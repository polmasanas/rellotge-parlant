package cat.rellotgeparlant.speech

import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale
import java.util.UUID

/** TextToSpeech in Catalan on the alarm stream, preferring Google's engine (Samsung's has no Catalan). */
class AndroidTtsEngine(context: Context) : TtsEngine {

    private val appContext = context.applicationContext
    private val audioManager = appContext.getSystemService(AudioManager::class.java)
    private var tts: TextToSpeech? = null
    private var volume = 1f

    private val attributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_ALARM)
        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
        .build()

    private val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
        .setAudioAttributes(attributes)
        .build()

    override fun init(onReady: (Boolean) -> Unit) {
        val listener = TextToSpeech.OnInitListener { status ->
            val ok = status == TextToSpeech.SUCCESS
            if (ok) configure()
            onReady(ok)
        }
        tts = if (isGoogleEngineInstalled(appContext)) {
            TextToSpeech(appContext, listener, GOOGLE_TTS)
        } else {
            TextToSpeech(appContext, listener)
        }
    }

    private fun configure() {
        val t = tts ?: return
        t.setAudioAttributes(attributes)
        t.language = CATALAN
        t.setSpeechRate(DEFAULT_RATE)
        t.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) = Unit
            override fun onDone(utteranceId: String?) { audioManager.abandonAudioFocusRequest(focusRequest) }
            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) { audioManager.abandonAudioFocusRequest(focusRequest) }
        })
    }

    override fun isCatalanAvailable(): Boolean {
        val t = tts ?: return false
        return t.isLanguageAvailable(CATALAN) >= TextToSpeech.LANG_AVAILABLE
    }

    override fun speak(text: String, flush: Boolean) {
        val t = tts ?: return
        // Focus is refused during a call or while ringing: a second guard against talking over a call.
        if (audioManager.requestAudioFocus(focusRequest) != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) return
        val mode = if (flush) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD
        val params = android.os.Bundle().apply { putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, volume) }
        t.speak(text, mode, params, UUID.randomUUID().toString())
    }

    override fun setRate(rate: Float) {
        tts?.setSpeechRate(rate)
    }

    override fun setVolume(volume: Float) {
        this.volume = volume.coerceIn(0f, 1f)
    }

    override fun shutdown() {
        tts?.shutdown()
        tts = null
    }

    companion object {
        fun isGoogleEngineInstalled(context: Context): Boolean =
            context.packageManager
                .queryIntentServices(Intent(TextToSpeech.Engine.INTENT_ACTION_TTS_SERVICE), 0)
                .any { it.serviceInfo.packageName == GOOGLE_TTS }

        const val GOOGLE_TTS = "com.google.android.tts"
        const val DEFAULT_RATE = 0.85f
        val CATALAN: Locale = Locale.forLanguageTag("ca-ES")
    }
}
