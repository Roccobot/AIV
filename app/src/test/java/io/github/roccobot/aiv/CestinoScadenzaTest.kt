package io.github.roccobot.aiv

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Che cosa lo svuotamento automatico del cestino può togliere, e che cosa non deve toccare mai.
 *
 * ⚠️⚠️ **QUESTA PROVA ESISTE PERCHÉ LA FUNZIONE CANCELLA FILE PER SEMPRE**: è l'unica parte
 * dell'app che elimina qualcosa senza che nessuno tocchi niente in quel momento, quindi un
 * difetto qui non si vede e non si può disfare. Il criterio universale sta in `AIV/CLAUDE.md`
 * § '🧪 Quando si scrive una prova, e quando no': una modifica che può fare danni porta la sua
 * prova anche senza un difetto alle spalle.
 *
 * ⚠️ **Gira su una JVM normale, senza Robolectric**: [Bin.expiring] è pura per costruzione, e
 * la sola cosa che tocca il disco (se l'originale esiste ancora) le arriva come una funzione.
 * È la stessa scelta di `Bin.ordered` e `Bin.records`.
 */
class CestinoScadenzaTest {

    /** **Con lo svuotamento spento non scade niente**, che è il valore di fabbrica. */
    @Test
    fun `a zero giorni non si tocca niente`() {
        val righe = listOf(
            Bin.Record("vecchia.jpg", ADESSO - GIORNO * 400, "/foto/vecchia.jpg", Bin.KIND_SENT)
        )
        assertEquals(emptyList<String>(), Bin.expiring(righe, 0, ADESSO) { false })
    }

    /**
     * **Un file eliminato scade quando ha compiuto il suo tempo, e non prima.**
     *
     * ⚠️ Le due righe servono insieme: senza la seconda, una funzione che restituisse tutto
     * passerebbe la prima.
     */
    @Test
    fun `scade solo chi ha passato i giorni scelti`() {
        val righe = listOf(
            Bin.Record("ieri.jpg", ADESSO - GIORNO, "/foto/ieri.jpg", Bin.KIND_SENT),
            Bin.Record("vecchia.jpg", ADESSO - GIORNO * 40, "/foto/vecchia.jpg", Bin.KIND_SENT)
        )
        assertEquals(listOf("vecchia.jpg"), Bin.expiring(righe, 30, ADESSO) { false })
    }

    /**
     * **Una copia di sicurezza dell'editor non scade mai**, ed è la decisione dell'utente
     * (giro della 1.67, `d-cestino-editor`: `fuori`).
     *
     * ⚠️⚠️ **L'ORIGINALE QUI NON C'È PIÙ, ed è il caso che rende necessaria la quarta colonna**:
     * chi modifica una fotografia e poi la elimina si ritrova una copia di sicurezza orfana, e
     * senza il marcatore la si scambierebbe per un'eliminazione qualunque. È l'unico file
     * rimasto con la versione di prima della modifica.
     */
    @Test
    fun `una copia di sicurezza non scade nemmeno se l'originale è sparito`() {
        val righe = listOf(
            Bin.Record("copia.jpg", ADESSO - GIORNO * 400, "/foto/copia.jpg", Bin.KIND_KEPT)
        )
        assertEquals(emptyList<String>(), Bin.expiring(righe, 7, ADESSO) { false })
    }

    /**
     * **Una riga scritta prima della `1.75` non porta il marcatore**, e allora decide se il file
     * d'origine esiste ancora: se c'è, è una copia di sicurezza e resta.
     */
    @Test
    fun `senza marcatore un originale ancora presente protegge il file`() {
        val righe = listOf(
            Bin.Record("vecchia.jpg", ADESSO - GIORNO * 40, "/foto/c-e-ancora.jpg"),
            Bin.Record("sparita.jpg", ADESSO - GIORNO * 40, "/foto/sparita.jpg")
        )
        val esiti = Bin.expiring(righe, 30, ADESSO) { path -> path == "/foto/c-e-ancora.jpg" }
        assertEquals(listOf("sparita.jpg"), esiti)
    }

    /**
     * **La quarta colonna sopravvive al giro di scrittura e rilettura dell'archivio.**
     *
     * ⚠️⚠️ **E UNA RIGA A TRE COLONNE SI LEGGE ANCORA**: è l'archivio di chiunque aggiorni
     * dalla 1.74, e rifiutarla vorrebbe dire che ogni file già nel cestino perde la sua
     * provenienza, cioè non si può più ripristinare.
     */
    @Test
    fun `l'archivio regge le righe vecchie e quelle nuove`() {
        val vecchia = "vecchia.jpg\t1000\t/foto/vecchia.jpg"
        val nuova = "nuova.jpg\t2000\t/foto/nuova.jpg\t${Bin.KIND_KEPT}"
        val lette = Bin.records("$vecchia\n$nuova")

        assertEquals(2, lette.size)
        assertEquals(null, lette[0].kind)
        assertEquals(Bin.KIND_KEPT, lette[1].kind)
        // ⚠️ Il giro completo, e non solo la lettura: una riga vecchia deve tornare a tre
        // colonne, o il formato nuovo e quello vecchio diventerebbero due modi di dire la
        // stessa cosa.
        assertEquals("$vecchia\n$nuova", Bin.text(lette))
    }
}

/** Un istante qualunque, scritto e non chiesto all'orologio: una prova non deve dipendere da lui. */
private const val ADESSO = 1_800_000_000_000L

/** Quanto dura un giorno, per leggere le età delle righe qui sopra a colpo d'occhio. */
private const val GIORNO = 24L * 60L * 60L * 1000L
