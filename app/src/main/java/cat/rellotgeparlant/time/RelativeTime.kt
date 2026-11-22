package cat.rellotgeparlant.time

/** "d'aquí dues hores i quinze minuts": how long until something, in Catalan words. */
object RelativeTime {

    private val UNITS = listOf("", "un", "dos", "tres", "quatre", "cinc", "sis", "set", "vuit", "nou")
    private val TEENS = listOf("deu", "onze", "dotze", "tretze", "catorze", "quinze", "setze", "disset", "divuit", "dinou")
    private val TENS = mapOf(2 to "vint", 3 to "trenta", 4 to "quaranta", 5 to "cinquanta", 6 to "seixanta", 7 to "setanta", 8 to "vuitanta", 9 to "noranta")

    fun phrase(minutesUntil: Long): String {
        if (minutesUntil < 1) return "ara mateix"
        val hours = (minutesUntil / 60).toInt()
        val minutes = (minutesUntil % 60).toInt()
        val parts = buildList {
            if (hours > 0) add("${number(hours, feminine = true)} ${if (hours == 1) "hora" else "hores"}")
            if (minutes > 0) add("${number(minutes, feminine = false)} ${if (minutes == 1) "minut" else "minuts"}")
        }
        return "d'aquí " + parts.joinToString(" i ")
    }

    /** 1–99 in words; "una/dues" agree with feminine nouns (hores). */
    private fun number(n: Int, feminine: Boolean): String {
        require(n in 1..99) { "out of range: $n" }
        fun unit(u: Int) = when {
            feminine && u == 1 -> "una"
            feminine && u == 2 -> "dues"
            else -> UNITS[u]
        }
        return when {
            n < 10 -> unit(n)
            n < 20 -> TEENS[n - 10]
            n % 10 == 0 -> TENS.getValue(n / 10)
            n < 30 -> "vint-i-${unit(n % 10)}"
            else -> "${TENS.getValue(n / 10)}-${unit(n % 10)}"
        }
    }
}
