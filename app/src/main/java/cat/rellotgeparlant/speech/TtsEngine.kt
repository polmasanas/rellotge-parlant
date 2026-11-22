package cat.rellotgeparlant.speech

/** Thin seam over Android's TextToSpeech so Speaker can be unit-tested. */
interface TtsEngine {
    fun init(onReady: (Boolean) -> Unit)
    fun isCatalanAvailable(): Boolean
    fun speak(text: String, flush: Boolean)
    fun setRate(rate: Float)
    fun setVolume(volume: Float)
    fun shutdown()
}
