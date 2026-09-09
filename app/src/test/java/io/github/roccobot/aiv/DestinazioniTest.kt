package io.github.roccobot.aiv

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Il banco di prova delle **cartelle che si possono usare come destinazione**.
 *
 * ⚠️⚠️ **NASCE COL PUNTO F DEL GIRO DELLA `1.80`** (*voglio usare la normale vista già attiva
 * per l'uso normale di AIV, e voglio che la copia o lo spostamento avvengano direttamente al
 * tocco della cartella destinazione*), e prova il pezzo in cui un errore **manda dei file in un
 * posto sbagliato**: quale cartella entra nell'elenco che si tocca.
 *
 * ⚠️ **Quello che il banco non vede**: la vista, cioè che le copertine e i conti siano quelli
 * scelti dall'utente, e che un tocco parta senza conferma. Quello lo dice il telefono, e la voce
 * di collaudo lo chiede.
 */
@RunWith(AndroidJUnit4::class)
class DestinazioniTest {

    private val cestino = "/storage/emulated/0/Android/media/io.github.roccobot.aiv/bin"

    private fun cartella(nome: String, path: String?) =
        Folder.Bucket(id = nome.hashCode().toLong(), name = nome, pictures = 3, clips = 0, cover = null, path = path)

    /**
     * **Caso 1: senza percorso non è una destinazione.**
     *
     * ⚠️ Una copia scrive su **disco**: la colonna che dà il percorso di una cartella del
     * MediaStore può mancare, e una riga toccabile che non porta da nessuna parte è peggio di una
     * riga che non c'è.
     */
    @Test
    fun `una cartella senza percorso non e una destinazione`() {
        val elenco = listOf(
            cartella("Camera", "/storage/emulated/0/DCIM/Camera"),
            cartella("Senza", null)
        )
        assertEquals(
            listOf("Camera"),
            destinations(elenco, cestino, emptySet()).map { it.name }
        )
    }

    /**
     * **Caso 2: il cestino non è una destinazione, e nemmeno quello che ci sta dentro.**
     *
     * ⚠️⚠️ **Copiarci dentro vuol dire mettere un file in un posto che si svuota, e spostarcelo
     * è eliminarlo passando dalla porta di servizio**: senza la conferma e senza che l'archivio
     * delle provenienze ne sappia niente.
     */
    @Test
    fun `il cestino non e una destinazione`() {
        val elenco = listOf(
            cartella("Bin", cestino),
            cartella("Dentro", "$cestino/2026"),
            cartella("Camera", "/storage/emulated/0/DCIM/Camera")
        )
        assertEquals(listOf("Camera"), destinations(elenco, cestino, emptySet()).map { it.name })
    }

    /**
     * **Caso 3: una cartella che comincia come il cestino ma non è dentro di lui resta.**
     *
     * ⚠️ È il confronto che si sbaglia scrivendo un `startsWith` senza la barra: `bin-vecchio`
     * comincia per `bin` e non sta nel cestino. Con quel difetto una cartella vera sparirebbe
     * dall'elenco senza che nessuno capisca perché.
     */
    @Test
    fun `una cartella che somiglia al cestino resta`() {
        val elenco = listOf(cartella("Quasi", "${cestino}-vecchio"))
        assertEquals(listOf("Quasi"), destinations(elenco, cestino, emptySet()).map { it.name })
    }

    /**
     * **Caso 4: una cartella nascosta non è una destinazione, e nemmeno quello che ci sta
     * dentro.**
     *
     * ⚠️⚠️ **È LA SUA RICHIESTA DELLA `2.02`** (*le cartelle nascoste devono rimanere nascoste
     * anche quando si copiano/spostano file*): fino alla `2.01` l'esclusione valeva per la
     * schermata iniziale e non per questa finestra, quindi una cartella tolta dall'elenco di casa
     * ricompariva appena si toccava 'Copia'.
     * ⚠️ **Il ramo intero**, come per il cestino: chi esclude un percorso esclude quello che sta
     * sotto, e il confronto è sul separatore.
     */
    @Test
    fun `una cartella nascosta non e una destinazione`() {
        val nascosta = "/storage/emulated/0/Privato"
        val elenco = listOf(
            cartella("Privato", nascosta),
            cartella("Dentro", "$nascosta/2026"),
            // ⚠️ Comincia come la nascosta e non ci sta dentro: è il caso che un `startsWith`
            // senza barra si porterebbe via, cioè lo stesso difetto del caso 3.
            cartella("Privatissimo", "${nascosta}ne"),
            cartella("Camera", "/storage/emulated/0/DCIM/Camera")
        )
        assertEquals(
            listOf("Privatissimo", "Camera"),
            destinations(elenco, cestino, setOf(nascosta)).map { it.name }
        )
    }

    /**
     * **Caso 5: l'ordine non si tocca.**
     *
     * ⚠️ L'elenco arriva con la cartella toccata più di recente per prima, che è la ragione per
     * cui la scorciatoia è comoda: riordinarlo qui vorrebbe dire perdere proprio quello.
     */
    @Test
    fun `l'ordine resta quello che arriva`() {
        val elenco = listOf(
            cartella("Terza", "/storage/emulated/0/C"),
            cartella("Prima", "/storage/emulated/0/A"),
            cartella("Seconda", "/storage/emulated/0/B")
        )
        assertEquals(
            listOf("Terza", "Prima", "Seconda"),
            destinations(elenco, cestino, emptySet()).map { it.name }
        )
    }
}
