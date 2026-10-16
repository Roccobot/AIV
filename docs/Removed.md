# Removed.md: le cose tolte da AIV, e come si recuperano

> **Cos'è questo file.** L'archivio di quello che è uscito dall'app: per ogni cosa tolta, che cosa
> faceva, quando è stata spenta e tolta, e da quale tag o commit si recupera. Si legge su richiesta,
> quando qualcuno vuole riportare indietro una funzione o capire dove viveva.
> ⚠️ **Nei file di regole resta solo la decisione** (che la cosa non c'è più, e perché): ogni
> appunto su come recuperarla vive qui, corto o lungo che sia (regola universale in
> `rules/Roccobot.md` di `Roccobot/tools`, § '🗄️ Le cose tolte vivono in Removed.md').
> Le voci vanno dalla più recente alla più vecchia.

## 6.30

### 🖼️ Il bordo d'accento dei pannelli

- **Che cosa faceva**: un contorno nel colore d'accento intorno a menu, dialoghi, schede e
  notifiche, disegnato sopra la superficie senza occupare spazio.
- **Spento dalla `4.38`** (scelta dell'utente del 2026-10-06, variante A, i pannelli nudi),
  **tolto nella `6.30`** (scelta C1 sulla voce G2 della revisione Setaccio, 2026-10-16).
- **Si recupera dal tag `v6.29`**. Dove viveva:
  - `Edge.kt`: `edged`, `edgedTop` e la costante `ACCENT_EDGE`, che lo accendeva;
  - i chiamanti, cioè `ActionPad.kt`, `Centred.kt`, `EditorScreen.kt`, `Menus.kt`, `Notice.kt`,
    `Sheet.kt`, `Theme.kt`, `Veil.kt` e `ViewerActivity.kt`;
  - la prova `BordoTest.kt`;
  - le note di `Rules.md` § '🌫️ Sfocatura dietro i pannelli e bordo'.
- Il comando che mostra il codice di allora: `git show v6.29:app/src/main/java/io/github/roccobot/aiv/Edge.kt`.

### 📁 La voce 'Copertina predefinita' del menu del FAB

- **Che cosa faceva**: dal menu della griglia rimetteva come copertina della cartella la sua
  immagine più recente.
- **Spenta dalla `1.95`** (voce `copertina-togli` dell'utente: un tocco involontario toglieva una
  scelta che non si ritrova), **tolta nella `6.30`** (scelta C1, 2026-10-16). Al suo posto c'è il
  gesto sull'icona dell'intestazione.
- **Si recupera dal tag `v6.29`**. Dove viveva:
  - `FolderCover.kt`: la costante `COVER_MENU_ROW`, che la accendeva, e `FolderCovers.of`;
  - `GridScreen.kt`: la voce del menu, con `onCoverClear` e `coverSet`;
  - `ViewerViewModel.clearCover`, e lo stato `cover` che leggeva;
  - la stringa `folder_cover_auto`, nelle 28 lingue, e la sua riga in `docs/Labels-approved.json`;
  - le note di `docs/Folders.md` sulla copertina e la parte di `CopertinaTest.kt` che la provava.
