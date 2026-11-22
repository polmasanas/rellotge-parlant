package cat.rellotgeparlant.speech

class FakeTtsEngine(var catalan: Boolean = true) : TtsEngine {
    var onReady: ((Boolean) -> Unit)? = null
    val spoken = mutableListOf<Pair<String, Boolean>>()
    var rate: Float? = null
    var volume: Float? = null
    var initCount = 0

    override fun init(onReady: (Boolean) -> Unit) { this.onReady = onReady; initCount++ }
    override fun isCatalanAvailable() = catalan
    override fun speak(text: String, flush: Boolean) { spoken += text to flush }
    override fun setRate(rate: Float) { this.rate = rate }
    override fun setVolume(volume: Float) { this.volume = volume }
    override fun shutdown() {}

    /** Like the real engine, initialisation resets the rate to the engine default. */
    fun ready(ok: Boolean = true) { rate = null; volume = null; onReady!!.invoke(ok) }
}
