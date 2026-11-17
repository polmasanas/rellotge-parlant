package cat.rellotgeparlant.speech

enum class VoiceStatus { READY, INITIALISING, MISSING }

/**
 * Speaks Catalan text. Text requested before the engine is ready is kept (latest only)
 * and spoken once ready. If no Catalan voice exists it stays silent: never another language.
 */
class Speaker(private val engine: TtsEngine) {

    @Volatile var status: VoiceStatus = VoiceStatus.INITIALISING
        private set

    private var pending: String? = null

    init {
        engine.init { ok -> onEngineReady(ok) }
    }

    @Synchronized
    private fun onEngineReady(ok: Boolean) {
        status = if (ok && engine.isCatalanAvailable()) VoiceStatus.READY else VoiceStatus.MISSING
        val text = pending
        pending = null
        if (status == VoiceStatus.READY && text != null) engine.speak(text, flush = true)
    }

    @Synchronized
    fun say(text: String) {
        when (status) {
            VoiceStatus.READY -> engine.speak(text, flush = true)
            VoiceStatus.INITIALISING -> pending = text
            VoiceStatus.MISSING -> Unit
        }
    }

    fun setRate(rate: Float) = engine.setRate(rate)

    fun shutdown() = engine.shutdown()
}
