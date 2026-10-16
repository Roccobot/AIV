package io.github.roccobot.aiv

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.xmlpull.v1.XmlPullParser

/**
 * Che cosa salva il backup di Android, dalla `6.29` (rilievi F11, F12 ed E1 della revisione della
 * 6.25, voci B7 e B8 del rapporto): le impostazioni e le cose fatte dall'utente, nelle tre sezioni
 * dei due file, e nessuna misura del telefono.
 *
 * ⚠️ **Si leggono i due file compilati**, come li legge il sistema, e non il sorgente.
 */
@RunWith(AndroidJUnit4::class)
class BackupSistemaTest {

    private val atteso = listOf(
        "file:datastore", "file:presets.json", "file:watermark", "file:covers"
    )

    /** Le righe `include` di ogni sezione del file [id], come `dominio:percorso`. */
    private fun sezioni(id: Int): Map<String, List<String>> {
        val app = ApplicationProvider.getApplicationContext<Context>()
        val xml = app.resources.getXml(id)
        val fuori = mutableMapOf<String, MutableList<String>>()
        var sezione = ""
        while (xml.next() != XmlPullParser.END_DOCUMENT) {
            if (xml.eventType != XmlPullParser.START_TAG) continue
            when (xml.name) {
                "include", "exclude" -> fuori.getOrPut(sezione) { mutableListOf() }.add(
                    (if (xml.name == "exclude") "-" else "") +
                        xml.getAttributeValue(null, "domain") + ":" + xml.getAttributeValue(null, "path")
                )
                else -> sezione = xml.name
            }
        }
        return fuori
    }

    /**
     * **Le tre sezioni salvano impostazioni, stili, logo e copertine, e nient'altro.**
     *
     * ⚠️ **Controprovata** coi due file della 6.28: c'erano solo `datastore` e `sharedpref`.
     */
    @Test
    fun `il backup salva quello che il file di impostazioni chiama impostazioni`() {
        val vecchio = sezioni(R.xml.backup_rules)
        val nuovo = sezioni(R.xml.data_extraction_rules)
        assertEquals(mapOf("full-backup-content" to atteso), vecchio)
        assertEquals(mapOf("cloud-backup" to atteso, "device-transfer" to atteso), nuovo)
    }
}
