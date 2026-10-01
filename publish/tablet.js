"use strict";
const routes = [
  [
    "folders",
    "Cartelle",
    "Elenco delle cartelle a lato e contenuto della cartella scelta nello spazio principale. Si cambia cartella senza perdere il contesto.",
    "La doppia vista introduce un comportamento da concordare; la selezione dei file resta nel contenuto.",
  ],
  [
    "grid",
    "Griglia dei media",
    "Più miniature leggibili, con selezione e comandi sempre raggiungibili. La densità si adatta alla finestra.",
    "La quantità di colonne resta una preferenza; il mockup mostra una densità dimostrativa.",
  ],
  [
    "system",
    "Cartelle di sistema",
    "Gerarchia a lato, file e sottocartelle nel pannello principale. Percorso e filtro visibili.",
    "Le autorizzazioni restano quelle del sistema; nessun accesso aggiuntivo implicito.",
  ],
  [
    "search",
    "Ricerca",
    "Ambito e ricerca in alto, risultati nello spazio ampio; i nomi restano leggibili.",
    "La ricerca conserva i filtri di cartelle incluse/escluse e l'esclusione dei PSD.",
  ],
  [
    "viewer",
    "Visualizzatore e video",
    "Immagine ampia al centro, informazioni a lato e comandi lungo il bordo. Il video usa lo stesso spazio.",
    "Nella finestra ridotta le informazioni tornano a richiesta; niente pannello sopra l'immagine durante i gesti.",
  ],
  [
    "simple",
    "Editor semplice",
    "Anteprima continua e rotazione, ritaglio e dimensioni nel pannello a lato.",
    "La colonna si sposta sotto l'immagine in verticale; la selezione dei comandi rimane.",
  ],
  [
    "editor",
    "Editor completo",
    "Immagine e parametri del modulo scelto insieme. Nove moduli raggiungibili senza coprire la foto.",
    "Un modulo alla volta. Le scelte sopravvivono a orientamento, tema e posizione dei pulsanti nel mockup.",
  ],
  [
    "settings",
    "Impostazioni",
    "Indice a lato e pagina selezionata a destra. Percorso e ricerca restano visibili.",
    "Il contenuto segue la proposta di riordino, da concordare prima dell'implementazione.",
  ],
  [
    "bin",
    "Cestino",
    "Anteprime a lato dei dettagli; selezione, ripristino ed eliminazione lungo il bordo.",
    "Il mockup non cancella file. Il cestino mantiene le sue conferme e la conservazione impostata.",
  ],
  [
    "history",
    "Cronologia dei ripristini",
    "Gruppi per data e percorsi nella stessa vista, con più spazio per i nomi lunghi.",
    "Rimane la cronologia dei soli ripristini, con la scadenza esistente.",
  ],
  [
    "destination",
    "Copia e sposta",
    "Cartelle e destinazione scelta visibili insieme, con conferma del percorso.",
    "Sfoglia tutte le cartelle resta un comando esplicito.",
  ],
  [
    "dialogs",
    "Rinomina, download e conversione",
    "Finestre centrate, di larghezza contenuta, con campo del nome, percorso e conferma leggibili.",
    "Le finestre scorrono con testo grande; estensione e formato conservano le proprie regole.",
  ],
  [
    "info",
    "Informazioni sul file",
    "Scheda laterale ampia con campi ordinati e percorso copiabile.",
    "Si richiude per recuperare tutto lo spazio dell'immagine.",
  ],
  [
    "resize",
    "Dimensioni e filigrana",
    "Anteprima accanto ai parametri, proporzioni e risultato ben leggibili.",
    "Il mockup comprende anche la scelta dell'editor e le finestre di salvataggio; nessuna modifica alla filigrana approvata.",
  ],
  [
    "help",
    "Guida e avvisi",
    "Spiegazione contenuta, esempio e comando finale in una finestra scorrevole.",
    "Permessi, caricamento, elenchi vuoti ed errori sono consultabili con il selettore Stato.",
  ],
];
const settings = {
  "Pagina iniziale": [
    "Aspetto",
    "Navigazione",
    "Comandi e indicatori",
    "Modifica e protezione dei file",
    "All'avvio",
    "Funzionalità avanzate",
    "Gestione dell'app",
  ],
  Cartelle: [
    "Intestazione delle cartelle",
    "Colore delle cartelle",
    "Opzioni di visualizzazione",
    "Cartelle incluse/escluse",
  ],
  "Opzioni di visualizzazione": [
    "Colonne delle griglie",
    "Contatore sotto le cartelle",
    "Nome del file in vista griglia",
    "Contatore elementi",
    "Dimensione del testo",
    "Lista file: copia il percorso della cartella contenitore",
    "Mostra i file nascosti",
    "Solo cartelle con immagini/video",
  ],
  "Cartelle incluse/escluse": [
    "Modalità escluse",
    "Modalità incluse",
    "Camera",
    "Screenshots",
    "Movies",
    "Aggiungi cartella",
  ],
  Visualizzatore: [
    "Sfondo del visualizzatore",
    "Tema dello sfondo",
    "Adattamento e zoom",
    "Riproduci i video direttamente",
  ],
  "Adattamento e zoom": [
    "Ingrandisci le immagini piccole",
    "Zoom massimo",
    "Rendering dei pixel",
    "Zoom nel menu del visualizzatore",
  ],
  Informazioni: [
    "Barra delle info",
    "Posizione della barra",
    "Contatore dei fotogrammi",
    "Info sul file singolo",
    "Dimensioni dei file selezionati",
  ],
  "Info sul file singolo": [
    "Nome del file",
    "Dimensioni",
    "Formato",
    "Peso",
    "Percorso",
  ],
  "Etichette e pulsanti": [
    "Posizione preferita dei pulsanti",
    "Etichette sotto le icone",
    "Indicatore dell'ultimo media visualizzato",
    "Ordine dei pulsanti",
  ],
  "Ordine dei pulsanti": [
    "Menu delle cartelle",
    "Selezione",
    "Visualizzatore",
    "Navigazione",
    "Modifica",
  ],
  "Editor e salvataggio": [
    "Editor immagini",
    "Copia di sicurezza prima di sovrascrivere",
    "Formato e qualità",
    "Stili dell'editor",
    "Filigrana",
  ],
  "Stili dell'editor": ["Naturale", "Luminoso", "Importa stili"],
  Filigrana: ["Immagine della filigrana", "Dimensione", "Opacità", "Posizione"],
  "Rinomina e download": [
    "Consenti la rinomina al salvataggio",
    "Scegli il percorso di download",
    "Consenti la modifica dell'estensione",
  ],
  "Gestisci le miniature memorizzate": [
    "Miniature memorizzate: 18 MB",
    "Genera le miniature",
    "Svuota la cache",
  ],
  "Esporta e importa": [
    "Aspetto",
    "Cartelle incluse/escluse",
    "Editor e salvataggio",
    "Cestino",
    "Esporta",
    "Importa",
  ],
};
const modules = {
  Dettaglio: ["Nitidezza", "Riduzione del rumore"],
  Effetti: ["Foschia", "Grana", "Vignettatura"],
  Geometria: ["Rotazione", "Prospettiva", "Fluidifica"],
  Ritaglio: ["Proporzioni", "Raddrizza"],
  Luce: ["Esposizione", "Contrasto", "Luci", "Ombre"],
  Colore: ["Temperatura", "Tinta", "Saturazione"],
  HSL: ["Tonalità", "Saturazione", "Luminosità"],
  Curve: ["Canale", "Punti della curva"],
  Stili: ["Naturale", "Luminoso", "Importa"],
};
const memory = {
  folder: "Camera",
  module: "Luce",
  settings: "Pagina iniziale",
  dialog: "Rinomina",
  values: {},
  brush: false,
};
const tablet = document.querySelector("#tablet"),
  content = document.querySelector("#content");
function node(tag, text, cls) {
  const e = document.createElement(tag);
  if (text !== null && text !== undefined) e.textContent = text;
  if (cls) e.className = cls;
  return e;
}
function button(text, fn, active = false) {
  const b = node("button", text, active ? "active" : "");
  b.type = "button";
  if (fn) b.addEventListener("click", fn);
  return b;
}
function art(cls = "") {
  return node("div", null, "art " + cls);
}
function pane(cls = "") {
  const p = node("section", null, "pane " + cls);
  content.append(p);
  return p;
}
function row(parent, title, detail, action) {
  const r = node("div", null, "row"),
    t = node("div", title, "description");
  if (detail) t.append(node("small", detail));
  r.append(t);
  if (action) r.append(action);
  parent.append(r);
}
function gallery(parent, n = 9) {
  const g = node("div", null, "grid");
  for (let i = 0; i < n; i++) {
    const b = button("", () => {
      document.querySelector("#route").value = "viewer";
      render();
    });
    b.className = "tile";
    b.append(
      art(i % 2 ? "alternate" : ""),
      node("span", `Paesaggio_${String(i + 1).padStart(2, "0")}.png`),
    );
    g.append(b);
  }
  parent.append(g);
}
function folderSide() {
  const s = pane("side");
  s.append(node("h3", "Cartelle"));
  for (const f of ["Camera", "Screenshots", "Movies", "Pictures"])
    s.append(
      button(
        f,
        () => {
          memory.folder = f;
          render();
        },
        f === memory.folder,
      ),
    );
  return s;
}
function canvas(parent) {
  const c = node("div", null, "canvas");
  c.append(art(), node("span", "Anteprima dimostrativa", "badge"));
  parent.append(c);
  return c;
}
function slider(parent, name) {
  const l = node("label", name),
    r = node("input");
  r.type = "range";
  r.min = 0;
  r.max = 100;
  r.value = memory.values[name] ?? 50;
  r.setAttribute("aria-label", name);
  l.append(r);
  parent.append(l);
  r.addEventListener("input", () => {
    memory.values[name] = Number(r.value);
    if (name === "Dimensione del pennello") {
      const ring = document.querySelector(".brush");
      if (ring) {
        ring.classList.add("preview");
        ring.style.width = ring.style.height = 40 + Number(r.value) + "px";
      }
    }
  });
  r.addEventListener("change", () =>
    document.querySelector(".brush")?.classList.remove("preview"),
  );
}
function render() {
  const key = document.querySelector("#route").value,
    route = routes.find((r) => r[0] === key),
    size = document.querySelector("#size").value;
  tablet.className = `tablet ${size} ${document.querySelector("#theme").value} ${document.querySelector("#hand").value} ${document.querySelector("#text").value}`;
  document.querySelector("#layout-title").textContent = route[1];
  document.querySelector("#why").textContent = route[2];
  document.querySelector("#limits").textContent = route[3];
  const bar = document.querySelector("#app-bar");
  bar.replaceChildren(
    button("Cartelle", () => {
      document.querySelector("#route").value = "folders";
      render();
    }),
    node("h2", route[1]),
  );
  content.replaceChildren();
  const footer = document.querySelector("#app-footer");
  footer.replaceChildren(node("span", "Contenuti dimostrativi", "notice"));
  const state = document.querySelector("#state").value;
  if (state !== "ready") {
    const p = pane("empty");
    const texts = {
      empty: [
        "Nessun elemento",
        "Scegli un'altra cartella o modifica il filtro.",
      ],
      loading: ["Caricamento in corso", "L'elenco comparirà qui."],
      denied: [
        "Consenti l'accesso alle foto",
        "Apri le autorizzazioni per mostrare i tuoi file.",
      ],
      error: [
        "Impossibile leggere il file",
        "Il percorso potrebbe non essere disponibile.",
      ],
    };
    p.append(
      node("h3", texts[state][0]),
      node("p", texts[state][1]),
      button("Riprova", () => {
        document.querySelector("#state").value = "ready";
        render();
      }),
    );
    return;
  }
  if (["folders", "grid", "system", "search", "destination"].includes(key)) {
    if (key !== "grid") folderSide();
    const p = pane();
    if (key === "search") {
      const label = node("label", "Cerca nei media"),
        input = node("input");
      input.type = "search";
      input.placeholder = "Cerca un nome...";
      label.append(input);
      p.append(label);
      input.addEventListener("input", () => {
        for (const b of p.querySelectorAll(".tile"))
          b.hidden = !b.textContent
            .toLowerCase()
            .includes(input.value.toLowerCase());
      });
    }
    p.append(
      node(
        "h3",
        key === "system"
          ? "/storage/emulated/0/" + memory.folder
          : memory.folder,
      ),
      node("p", "9 immagini · 3 video", "notice"),
    );
    if (key === "destination") {
      for (const f of [
        "Camera",
        "Screenshots",
        "Pictures",
        "Sfoglia tutte le cartelle...",
      ])
        row(
          p,
          f,
          "Memoria interna",
          button("Scegli", () => {
            memory.folder = f;
            render();
          }),
        );
      footer.append(
        button("Conferma destinazione", () => {
          footer.append(node("span", "Destinazione scelta: " + memory.folder));
        }),
      );
    } else gallery(p);
    footer.append(
      button("Seleziona", () => {
        for (const b of p.querySelectorAll(".tile"))
          b.classList.toggle("active");
      }),
      button("Impostazioni", () => {
        document.querySelector("#route").value = "settings";
        render();
      }),
    );
  } else if (key === "viewer" || key === "info") {
    const c = canvas(content),
      s = pane("side viewer-side");
    s.append(node("h3", "Informazioni"));
    for (const [a, b] of [
      ["Nome", "Paesaggio_01.png"],
      ["Dimensioni", "4.032 × 3.024"],
      ["Peso", "1,8 MB"],
      ["Percorso", "Pictures"],
    ])
      row(s, a, b);
    const play = button("Riproduci video", () => {
      play.textContent =
        play.textContent === "Pausa" ? "Riproduci video" : "Pausa";
      c.querySelector(".badge").textContent =
        play.textContent === "Pausa"
          ? "Video dimostrativo · 00:12 / 00:42"
          : "Anteprima dimostrativa";
    });
    footer.append(
      play,
      button("Modifica", () => {
        document.querySelector("#route").value = "editor";
        render();
      }),
      button("Informazioni", () => {
        s.classList.toggle("viewer-side");
      }),
    );
  } else if (["editor", "simple", "resize"].includes(key)) {
    const ed = node("div", null, "editor");
    content.append(ed);
    const c = canvas(ed),
      tools = node("section", null, "tools");
    ed.append(tools);
    const menu = node("div", null, "modules");
    tools.append(menu);
    const choices =
      key === "editor"
        ? Object.keys(modules)
        : key === "simple"
          ? ["Geometria", "Ritaglio"]
          : ["Dimensioni", "Filigrana"];
    if (!choices.includes(memory.module)) memory.module = choices[0];
    for (const m of choices)
      menu.append(
        button(
          m,
          () => {
            memory.module = m;
            render();
          },
          m === memory.module,
        ),
      );
    const controls = node("div", null, "controls");
    controls.append(node("h3", memory.module));
    tools.append(controls);
    const names =
      modules[memory.module] ??
      (memory.module === "Dimensioni"
        ? ["Larghezza", "Altezza"]
        : ["Dimensione", "Opacità", "Posizione"]);
    for (const name of names) {
      if (name === "Fluidifica") {
        controls.append(
          button(
            "Fluidifica",
            () => {
              memory.brush = !memory.brush;
              render();
            },
            memory.brush,
          ),
        );
      } else slider(controls, name);
    }
    if (memory.module === "Geometria" && memory.brush) {
      slider(controls, "Dimensione del pennello");
      c.append(node("div", null, "brush"));
      controls.append(
        node(
          "p",
          "Regola la dimensione: cerchio spesso. Durante il gesto: cerchio sottile.",
          "notice",
        ),
      );
    }
    const compare = button("Confronta", () => c.classList.toggle("comparing"));
    footer.append(
      button("Annulla ultima modifica", () => {
        c.querySelector(".badge").textContent =
          "Ultima modifica annullata (esempio)";
      }),
      button("Ripeti modifica", () => {
        c.querySelector(".badge").textContent = "Modifica ripetuta (esempio)";
      }),
      compare,
      button("Salva", () => {
        document.querySelector("#route").value = "dialogs";
        memory.dialog = "Salva";
        render();
      }),
    );
  } else if (key === "settings") {
    const side = pane("side"),
      p = pane();
    side.append(node("h3", "Impostazioni"));
    for (const title of Object.keys(settings))
      side.append(
        button(
          title,
          () => {
            memory.settings = title;
            render();
          },
          title === memory.settings,
        ),
      );
    p.append(node("h3", memory.settings));
    for (const title of settings[memory.settings]) {
      if (settings[title])
        row(
          p,
          title,
          "Apri la pagina",
          button("Apri", () => {
            memory.settings = title;
            render();
          }),
        );
      else {
        const b = button("Scelta", () => {
          b.textContent = b.textContent === "Attiva" ? "Disattiva" : "Attiva";
        });
        row(p, title, "Valore dimostrativo", b);
      }
    }
    p.append(
      node("p", "Disposizione proposta, ancora da concordare.", "notice"),
    );
  } else if (["bin", "history"].includes(key)) {
    const p = pane();
    p.append(
      node(
        "h3",
        key === "history"
          ? "Oggi · 2 ripristini"
          : "7 file · eliminazione automatica attiva",
      ),
    );
    if (key === "bin") gallery(p, 6);
    else
      for (const n of [1, 2])
        row(
          p,
          "Paesaggio_0" + n + ".png",
          "Memoria interna/Pictures · 10:24",
          button("Apri", () => {
            document.querySelector("#route").value = "viewer";
            render();
          }),
        );
    footer.append(
      button(key === "bin" ? "Ripristina selezionati" : "Apri cartella", () => {
        footer.append(node("span", "Operazione dimostrativa"));
      }),
    );
  } else {
    const d = node("section", null, "dialog-preview");
    content.append(d);
    if (key === "dialogs") {
      const chips = node("div", null, "chips");
      for (const t of ["Rinomina", "Download", "Conversione", "Salva"])
        chips.append(
          button(
            t,
            () => {
              memory.dialog = t;
              render();
            },
            memory.dialog === t,
          ),
        );
      d.append(chips, node("h3", memory.dialog));
      const l = node("label", "Nome del file"),
        input = node("input");
      input.value = "Paesaggio_modificato.png";
      l.append(input);
      d.append(l, node("p", "Pictures · Memoria interna"));
      if (memory.dialog === "Conversione")
        d.append(node("p", "Formato: PNG · Senza perdita"));
    } else
      d.append(
        node("h3", "Scegli come aprire il file"),
        node(
          "p",
          "Puoi usare l'editor semplice oppure l'editor completo. La scelta viene ricordata se lo desideri.",
        ),
        button("Editor semplice", () => {
          document.querySelector("#route").value = "simple";
          render();
        }),
        button("Editor completo", () => {
          document.querySelector("#route").value = "editor";
          render();
        }),
      );
    d.append(node("div", null, "chips"));
    d.lastChild.append(
      button("Annulla", () => {
        document.querySelector("#route").value = "viewer";
        render();
      }),
      button("Conferma", () => {
        d.append(
          node("p", "Scelta confermata nel mockup. Nessun file modificato."),
        );
      }),
    );
  }
}
for (const r of routes) {
  const o = node("option", r[1]);
  o.value = r[0];
  document.querySelector("#route").append(o);
}
for (const select of document.querySelectorAll(".choices select"))
  select.addEventListener("change", render);
render();
