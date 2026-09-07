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
 * La schermata iniziale con gli argomenti minimi, cioè senza cartelle e senza permesso.
 *
 * ⚠️ **Quello che si guarda è il FAB, che c'è in ogni caso**: dipende dalla vista scelta e
 * non dai dati, quindi una casa vuota è la scena più piccola che lo contiene. Le cartelle vere
 * porterebbero le copertine, cioè il caricamento delle miniature, che su una macchina senza
 * telefono non porta niente in più e può soltanto fallire.
 */
@Composable
internal fun CasaVuota() {
    FolderScreen(
        view = FolderView.GRID,
        columns = 3,
        counted = true,
        hidden = emptySet(),
        onHide = {},
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
        buckets = emptyList(),
        onRead = {}
    )
}
