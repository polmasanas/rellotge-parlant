package cat.rellotgeparlant.calendar

import cat.rellotgeparlant.time.RelativeTime
import java.time.Duration
import java.time.LocalDateTime

/** Builds what is said about calendar events, in Catalan. */
object EventText {
    private const val MAX_EVENTS = 3
    private const val MAX_DESCRIPTION = 200
    private val TAG = Regex("<[^>]*>")
    private val URL = Regex("""(https?://\S+|www\.\S+)""")
    private val SPACES = Regex("""\s+""")
    private val SENTENCE_END = Regex("""(?<=[.!?])\s+""")

    fun cleanDescription(raw: String?): String? {
        if (raw == null) return null
        val text = raw.replace(TAG, " ").replace(URL, " ").replace(SPACES, " ").trim()
        if (text.isEmpty()) return null
        val twoSentences = text.split(SENTENCE_END).take(2).joinToString(" ")
        return if (twoSentences.length <= MAX_DESCRIPTION) twoSentences
        else twoSentences.take(MAX_DESCRIPTION - 1).trimEnd() + "…"
    }

    /** "D'aquí dues hores i quinze minuts: Metge. Lloc: …" — how long from [now] until the event. */
    fun describe(e: CalendarEvent, now: LocalDateTime, s: EventSettings): String {
        val until = RelativeTime.phrase(Duration.between(now, e.start).toMinutes())
        val parts = mutableListOf("${until.replaceFirstChar { it.uppercase() }}: ${sentence(e.title) ?: "un esdeveniment."}")
        if (s.readLocation) sentence(e.location)?.let { parts += "Lloc: $it" }
        if (s.readDescription) cleanDescription(e.description)?.let { parts += it }
        return parts.joinToString(" ")
    }

    fun upcomingSentence(events: List<CalendarEvent>, now: LocalDateTime, s: EventSettings): String? {
        if (events.isEmpty()) return null
        val said = events.take(MAX_EVENTS).joinToString(" ") { describe(it, now, s) }
        val more = events.size - MAX_EVENTS
        return if (more > 0) "$said I $more més." else said
    }

    fun morningSentence(allDay: List<CalendarEvent>): String? {
        if (allDay.isEmpty()) return null
        val titles = allDay.take(MAX_EVENTS).map { bare(it.title) ?: "un esdeveniment" }
        val more = allDay.size - MAX_EVENTS
        return "Bon dia. Avui: ${titles.joinToString(", ")}${if (more > 0) " i $more més" else ""}."
    }

    /** Trimmed text without a trailing full stop, or null if blank. */
    private fun bare(text: String?): String? = text?.trim()?.trimEnd('.')?.trim()?.ifEmpty { null }

    /** Trimmed text ending in exactly one full stop, or null if blank. */
    private fun sentence(text: String?): String? = bare(text)?.let { "$it." }
}
