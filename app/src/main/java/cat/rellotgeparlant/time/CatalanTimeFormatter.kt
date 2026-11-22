package cat.rellotgeparlant.time

/**
 * Says the time the traditional Catalan way ("quarts"), rounded down to the
 * nearest anchor, 12-hour, with the part of the day taken from the current hour.
 */
object CatalanTimeFormatter {

    private val HOUR_NAMES = listOf(
        "dotze", "una", "dues", "tres", "quatre", "cinc",
        "sis", "set", "vuit", "nou", "deu", "onze",
    )

    /** Anchors in ascending order; minutes are floored to the latest one. */
    private val ANCHORS = listOf(0, 5, 8, 10, 15, 20, 23, 25, 30, 35, 38, 40, 45, 50, 53, 55)

    /** Quarter phrases for anchors that refer to the next hour: (subject, plural). */
    private val QUARTERS = mapOf(
        8 to ("mig quart" to false),
        10 to ("un quart menys cinc" to false),
        15 to ("un quart" to false),
        20 to ("un quart i cinc" to false),
        23 to ("un quart i mig" to false),
        25 to ("dos quarts menys cinc" to true),
        30 to ("dos quarts" to true),
        35 to ("dos quarts i cinc" to true),
        38 to ("dos quarts i mig" to true),
        40 to ("tres quarts menys cinc" to true),
        45 to ("tres quarts" to true),
        50 to ("tres quarts i cinc" to true),
        53 to ("tres quarts i mig" to true),
    )

    fun phrase(hour: Int, minute: Int): String {
        val (body, plural) = body(hour, minute)
        return "${if (plural) "Són" else "És"} $body."
    }

    /** The same time as [phrase] but as a complement: "a dos quarts de sis de la tarda". */
    fun atPhrase(hour: Int, minute: Int): String = "a ${body(hour, minute).first}"

    /** The phrase without verb or final stop, and whether its subject is plural. */
    private fun body(hour: Int, minute: Int): Pair<String, Boolean> {
        require(hour in 0..23) { "hour out of range: $hour" }
        require(minute in 0..59) { "minute out of range: $minute" }

        val anchor = ANCHORS.last { it <= minute }
        val current = hour % 12
        val next = (hour + 1) % 12
        val part = dayPart(hour)

        return when (anchor) {
            0 -> "${withArticle(current)} $part" to (current != 1)
            5 -> "${withArticle(current)} i cinc $part" to (current != 1)
            55 -> "${withArticle(next)} menys cinc $part" to (next != 1)
            else -> {
                val (subject, plural) = QUARTERS.getValue(anchor)
                "$subject ${ofHour(next)} $part" to plural
            }
        }
    }

    private fun withArticle(h12: Int): String =
        if (h12 == 1) "la una" else "les ${HOUR_NAMES[h12]}"

    private fun ofHour(h12: Int): String {
        val name = HOUR_NAMES[h12]
        return if (name.first() in "aeiouàèéíòóú") "d'$name" else "de $name"
    }

    private fun dayPart(hour: Int): String = when (hour) {
        in 1..5 -> "de la matinada"
        in 6..11 -> "del matí"
        in 12..14 -> "del migdia"
        in 15..19 -> "de la tarda"
        in 20..21 -> "del vespre"
        else -> "de la nit"
    }
}
