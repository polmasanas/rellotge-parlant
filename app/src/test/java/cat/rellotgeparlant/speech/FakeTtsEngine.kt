package cat.rellotgeparlant.speech

class FakeTtsEngine(var catalan: Boolean = true) : TtsEngine {
    var onReady: ((Boolean) -> Unit)? = null
    val spoken = mutableListOf<Pair<String, Boolean>>()
    var rate: Float? = null

    override fun init(onReady: (Boolean) -> Unit) { this.onReady = onReady }
    override fun isCatalanAvailable() = catalan
    override fun speak(text: String, flush: Boolean) { spoken += text to flush }
    override fun setRate(rate: Float) { this.rate = rate }
    override fun shutdown() {}

    fun ready(ok: Boolean = true) = onReady!!.invoke(ok)
}
