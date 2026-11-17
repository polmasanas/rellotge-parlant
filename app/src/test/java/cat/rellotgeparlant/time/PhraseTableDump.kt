package cat.rellotgeparlant.time

import org.junit.Test
import java.io.File

/** Writes every phrase of the day to app/build/phrase-table.txt for the native-speaker review. */
class PhraseTableDump {
    @Test fun dump() {
        val out = File("build/phrase-table.txt")
        out.parentFile.mkdirs()
        out.writeText(buildString {
            for (h in 0..23) for (m in 0..59) {
                appendLine("%02d:%02d  %s".format(h, m, CatalanTimeFormatter.phrase(h, m)))
            }
        })
    }
}
