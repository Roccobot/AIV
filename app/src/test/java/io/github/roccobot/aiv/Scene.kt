package io.github.roccobot.aiv

import androidx.compose.runtime.Composable

/**
 * Le **scene condivise** fra le prove: le schermate vere, montate con gli argomenti minimi.
 *
 * ⚠️⚠️ **UN ELENCO DI ARGOMENTI SCRITTO DUE VOLTE È DUE ELENCHI CHE DIVERGONO**, ed è lo stesso
 * difetto che il censimento della UI ha censito nel codice vero: `FolderScreen` ne vuole
 * ventotto, quindi la seconda prova che la monta non se li riscrive, la chiama da qui. Il giorno
 * che quella firma cambia, il posto da correggere è uno.
 */

/**
 * La schermata iniziale con gli argomenti minimi.
 *
 * ⚠️ **Di serie è senza cartelle e senza permesso**: quello che le prime prove guardavano è il
 * FAB, che c'è in ogni caso perché dipende dalla vista scelta e non dai dati, quindi una casa
 * vuota era la scena più piccola che lo contenesse.
 * ⚠️⚠️ **DALLA `1.92` LE CARTELLE SI POSSONO PASSARE, e servono a chi misura lo scorrimento**:
 * senza righe non c'è niente da scorrere, quindi una prova sull'intestazione che si chiude non
 * avrebbe nessun gesto da fare. Le copertine chiedono le miniature al caricatore, che su una
 * macchina senza telefono non risponde: la cella resta vuota e il **nome** si legge lo stesso,
 * che è quello che una prova cerca nell'albero.
 *
 * @param buckets le cartelle finte da mostrare, vuote di serie.
 */
@Composable
internal fun Casa(
    buckets: List<Folder.Bucket> = emptyList(),
    /** I percorsi nascosti, per le prove di 'Mostra nascoste'. */
    hidden: Set<String> = emptySet(),
    /** Se le nascoste sono in scena col minuto in corso. */
    peeking: Boolean = false,
    onUnhide: (String) -> Unit = {}
) {
    FolderScreen(
        view = FolderView.GRID,
        columns = 3,
        counted = true,
        colour = FolderColour.NONE,
        tints = emptyMap(),
        hidden = hidden,
        onHide = {},
        peeking = peeking,
        onPeek = {},
        onUnhide = onUnhide,
        recents = emptyList(),
        onPick = {},
        onOpen = {},
        onOpenPage = {},
        onView = {},
        onForget = {},
        onSettings = {},
        onSearch = {},
        onBin = {},
        onColumns = {},
        onColour = {},
        listCount = true,
        listText = TextSize.NORMAL,
        treeHidden = false,
        treePictures = false,
        onListCount = {},
        onListText = {},
        onTreeHidden = {},
        onTreePictures = {},
        treePath = null,
        binOn = false,
        factFields = emptyList(),
        onTreePath = {},
        onTreeOpen = { _, _ -> },
        forStart = false,
        buckets = buckets,
        onRead = {}
    )
}
