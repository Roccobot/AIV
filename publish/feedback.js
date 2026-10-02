"use strict";
const spec = JSON.parse(document.querySelector("#feedback-data").textContent);
const outcomes = ["Tutto OK", "Accettabile", "Non approvato"];
const maxFile = 8 * 1024 * 1024,
  maxTotal = 20 * 1024 * 1024;
const allowedMime = ["image/png", "image/jpeg", "image/webp", "image/gif", "image/svg+xml", "application/zip"];
// Preserve the historical images key in JSON drafts; it also holds ZIP files.
const blank = () => ({
  schema: 1,
  project: "AIV",
  version: spec.version,
  installed: spec.version,
  device: "",
  tablet: "",
  notes: "",
  extra: { images: [] },
  entries: {},
  decisions: {},
  updated: null,
  completed: null,
});
let draft = blank(),
  db = null,
  revision = 0,
  loaded = false,
  saveQueue = Promise.resolve(),
  saveTimer = null,
  persistedRevision = 0,
  pendingSaves = 0,
  remoteReady = false,
  syncing = false;
const remote = window.feedbackRemote;
const saved = document.querySelector("#saved"),
  message = document.querySelector("#action-message");
function el(tag, text, cls) {
  const e = document.createElement(tag);
  if (text !== undefined) e.textContent = text;
  if (cls) e.className = cls;
  return e;
}
function entry(id) {
  return (
    draft.entries[id] ??
    (draft.entries[id] = { status: "", comment: "", images: [] })
  );
}
function attachmentEntry(card) {
  return card.classList.contains("extra") ? draft.extra : entry(card.dataset.id);
}
function usedAttachmentBytes() {
  return [draft.extra, ...Object.values(draft.entries)].reduce((sum, value) =>
    sum + value.images.reduce((bytes, file) => bytes + file.size, 0), 0);
}
function decision(id) {
  return (
    draft.decisions[id] ?? (draft.decisions[id] = { choice: "", comment: "" })
  );
}
function report(text, error = false) {
  message.textContent = text;
  message.classList.toggle("error", error);
}
function validate(raw) {
  if (
    !raw ||
    raw.schema !== 1 ||
    raw.project !== "AIV" ||
    !raw.entries ||
    typeof raw.entries !== "object" ||
    Array.isArray(raw.entries) ||
    !raw.decisions ||
    typeof raw.decisions !== "object" ||
    Array.isArray(raw.decisions)
  )
    throw Error("Formato JSON non riconosciuto.");
  const clean = blank();
  let total = 0;
  for (const key of ["version", "installed", "device", "notes"]) {
    if (typeof raw[key] !== "string" || raw[key].length > 100000)
      throw Error("Campo non valido: " + key);
    clean[key] = raw[key];
  }
  if (raw.tablet !== undefined) {
    if (typeof raw.tablet !== "string" || raw.tablet.length > 100000)
      throw Error("Campo non valido: tablet");
    clean.tablet = raw.tablet;
  }
  for (const key of ["updated", "completed"]) {
    if (
      raw[key] !== undefined &&
      raw[key] !== null &&
      (typeof raw[key] !== "string" || !Number.isFinite(Date.parse(raw[key])))
    )
      throw Error("Data non valida.");
    clean[key] = raw[key] ?? null;
  }
  if (
    Object.keys(raw.entries).length > 500 ||
    Object.keys(raw.decisions).length > 100
  )
    throw Error("Troppe voci.");
  function cleanAttachments(images) {
    return images.map((img) => {
      if (
        !img ||
        typeof img.name !== "string" ||
        img.name.length > 500 ||
        !allowedMime.includes(img.type) ||
        typeof img.data !== "string" ||
        !img.data.startsWith("data:" + img.type + ";base64,") ||
        !Number.isInteger(img.size) ||
        img.size < 1 ||
        img.size > maxFile
      )
        throw Error("Allegato non valido.");
      const encoded = img.data.split(",")[1];
      if (
        !/^[A-Za-z0-9+/]*={0,2}$/.test(encoded) ||
        encoded.length % 4 !== 0 ||
        encoded.length > Math.ceil(maxFile / 3) * 4
      )
        throw Error("Dati allegato non validi.");
      const bytes = atob(encoded).length;
      if (bytes !== img.size) throw Error("Dimensione allegato non valida.");
      total += bytes;
      if (total > maxTotal) throw Error("Gli allegati superano 20 MB.");
      return { name: img.name, type: img.type, size: img.size, data: img.data };
    });
  }
  if (raw.extra !== undefined) {
    if (!raw.extra || !Array.isArray(raw.extra.images) || raw.extra.images.length > 30)
      throw Error("Allegati delle osservazioni non validi.");
    clean.extra = { images: cleanAttachments(raw.extra.images) };
  }
  for (const [id, value] of Object.entries(raw.entries)) {
    if (
      !/^\d+\.\d+-\d+$/.test(id) ||
      !value ||
      typeof value.comment !== "string" ||
      value.comment.length > 100000 ||
      !Array.isArray(value.images) ||
      value.images.length > 30
    )
      throw Error("Risposta non valida.");
    const status =
      { OK: "Tutto OK", "Da correggere": "Non approvato", "Non provato": "" }[
        value.status
      ] ?? value.status;
    if (status !== "" && !outcomes.includes(status))
      throw Error("Esito non valido.");
    const images = cleanAttachments(value.images);
    clean.entries[id] = { status, comment: value.comment, images };
  }
  for (const [id, value] of Object.entries(raw.decisions)) {
    if (
      !/^d-[a-z0-9-]+$/.test(id) ||
      !value ||
      typeof value.choice !== "string" ||
      typeof value.comment !== "string" ||
      value.comment.length > 100000
    )
      throw Error("Decisione non valida.");
    const question = spec.decisions.find((q) => q.id === id);
    if (
      question &&
      value.choice !== "" &&
      !question.options.includes(value.choice)
    )
      throw Error("Scelta non valida.");
    clean.decisions[id] = { choice: value.choice, comment: value.comment };
  }
  return clean;
}
function refreshCounts() {
  document.querySelector(".extra").classList.toggle("has-response", Boolean(draft.notes.trim() || draft.extra.images.length));
  for (const card of document.querySelectorAll(".test")) {
    const value = entry(card.dataset.id);
    card.dataset.outcome = value.status;
    card.classList.toggle("has-response", Boolean(value.status || value.comment.trim() || value.images.length));
  }
  for (const card of document.querySelectorAll(".decision")) {
    const value = decision(card.dataset.id);
    card.classList.toggle("has-response", Boolean(value.choice || value.comment.trim()));
  }
  const counters = Object.fromEntries(outcomes.map((o) => [o, 0]));
  let done = 0;
  for (const item of spec.items) {
    const status = entry(item.id).status;
    if (status) {
      counters[status]++;
      done++;
    }
  }
  const counts = document.querySelector("#counts");
  counts.replaceChildren(
    el("span", `${done}/${spec.items.length} con risposta`),
  );
  for (const [status, count] of Object.entries(counters))
    counts.append(el("span", `${status}: ${count}`));
  counts.append(el("span", `Non provato: ${spec.items.length - done}`));
  document.querySelector("#progress").value = done;
  document.querySelector("#answered").textContent =
    `Risposte: ${done} su ${spec.items.length}.`;
  refreshNavigation();
}

function alignDocumentVersion() {
  // ⚠️ **Nuovo rilascio del documento: svuota i campi liberi** (giro 3.25-3.30, note A).
  // Modello telefono/tablet restano. Le risposte alle prove con ID ancora presenti restano.
  if (draft.version === spec.version) return;
  draft.notes = "";
  draft.extra = { images: [] };
  draft.completed = null;
  draft.version = spec.version;
  if (!draft.installed) draft.installed = spec.version;
}
function hydrate() {
  for (const card of document.querySelectorAll(".test")) {
    const value = entry(card.dataset.id);
    card.querySelector(".comment").value = value.comment;
    card.querySelector(".item-state").textContent =
      value.status || "Non provato";
    for (const b of card.querySelectorAll(".outcome"))
      b.setAttribute("aria-pressed", String(b.dataset.status === value.status));
    drawAttachments(card);
  }
  for (const card of document.querySelectorAll(".decision")) {
    const value = decision(card.dataset.id);
    card.querySelector("textarea").value = value.comment;
    for (const b of card.querySelectorAll(".decision-options button"))
      b.setAttribute("aria-pressed", String(b.dataset.choice === value.choice));
  }
  for (const key of ["device", "tablet", "installed", "notes"])
    document.querySelector("#" + key).value = draft[key];
  drawAttachments(document.querySelector(".extra"));
  window.feedbackFormatting?.refresh();
  refreshCounts();
}
function drawAttachments(card) {
  const list = card.querySelector(".image-list");
  list.replaceChildren();
  attachmentEntry(card).images.forEach((img, index) => {
    const figure = el("figure");
    if (img.type === "application/zip") {
      const download = el("a", "Scarica ZIP", "zip-download");
      download.href = img.data;
      download.download = img.name;
      figure.append(el("span", "ZIP", "file-kind"), el("figcaption", img.name), download);
    } else {
      const image = el("img");
      image.src = img.data;
      image.alt = img.name;
      figure.append(image, el("figcaption", img.name));
    }
    const remove = el("button", "Rimuovi allegato");
    remove.type = "button";
    remove.addEventListener("click", () => {
      attachmentEntry(card).images.splice(index, 1);
      drawAttachments(card);
      changed();
    });
    figure.append(remove);
    list.append(figure);
  });
}
function save() {
  clearTimeout(saveTimer);
  saveTimer = null;
  if (!loaded) return Promise.resolve(false);
  if (remote && !remoteReady) return Promise.resolve(false);
  pendingSaves++;
  const current = revision,
    snapshot = remote ? remote.snapshot(draft) : structuredClone(draft);
  snapshot.updated = new Date().toISOString();
  snapshot.version = spec.version;
  saved.textContent = "Salvataggio in corso...";
  saved.classList.remove("error");
  saveQueue = saveQueue
    .catch(() => {})
    .then(
      () => remote ? remote.save(snapshot) :
        new Promise((resolve, reject) => {
          if (!db) {
            reject(Error("Memoria del browser non disponibile."));
            return;
          }
          const transaction = db.transaction("drafts", "readwrite");
          transaction.objectStore("drafts").put(snapshot, "current");
          transaction.oncomplete = () => resolve();
          transaction.onerror = transaction.onabort = () =>
            reject(transaction.error ?? Error("Salvataggio interrotto."));
        }),
    )
    .then((result) => {
      if (result?.updated) snapshot.updated = result.updated;
      persistedRevision = current;
      if (current === revision) {
        draft.updated = snapshot.updated;
        saved.textContent =
          (remote ? "Salvato nel cloud: " : "Salvato in questo browser: ") +
          new Date(snapshot.updated).toLocaleString("it-IT");
      }
      return true;
    })
    .catch((error) => {
      remote?.failed(error);
      saved.textContent =
        "Non salvato: esporta il JSON prima di chiudere. " + error.message;
      saved.classList.add("error");
      return false;
    }).finally(() => { pendingSaves--; });
  return saveQueue;
}
function changed() {
  revision++;
  draft.completed = null;
  refreshCounts();
  document.querySelector("#summary").value = "";
  clearTimeout(saveTimer);
  saveTimer = setTimeout(save, 350);
  saved.textContent = "Modifiche da salvare...";
}
function summary() {
  const lines = [
    `Feedback AIV ${spec.version}`,
    `Versione installata: ${draft.installed || "Non indicata"}`,
    `Telefono: ${draft.device || "Non indicato"}`,
    `Tablet: ${draft.tablet || "Non indicato"}`,
    "",
  ];
  for (const item of spec.items) {
    const value = entry(item.id);
    lines.push(`${item.id} - ${item.title}: ${value.status || "Non provato"}`);
    if (value.comment) lines.push(value.comment);
    if (value.images.length)
      lines.push(
        `Allegati: ${value.images.map((i) => i.name).join(", ")} (consegnare con JSON o allegati)`,
      );
    lines.push("");
  }
  lines.push("Decisioni");
  for (const question of spec.decisions) {
    const value = decision(question.id);
    lines.push(`${question.id}: ${value.choice || "Da decidere"}`);
    if (value.comment) lines.push(value.comment);
  }
  lines.push(
    "",
    "Qualsiasi altra cosa",
    draft.notes || "Nessuna osservazione.",
  );
  if (draft.extra.images.length)
    lines.push("Allegati alle osservazioni: " + draft.extra.images.map((file) => file.name).join(", ") + " (consegnare con JSON o allegati)");
  return lines.join("\n");
}
async function copy() {
  const text = summary();
  document.querySelector("#summary").value = text;
  try {
    await navigator.clipboard.writeText(text);
    report("Riepilogo copiato. Incollalo in chat con gli eventuali allegati.");
  } catch {
    const area = document.querySelector("#summary");
    area.focus();
    area.select();
    report("Copia il testo selezionato e incollalo in chat.");
  }
}
for (const question of spec.decisions) {
  const card = el("article", undefined, "card decision");
  card.dataset.id = question.id;
  card.append(el("h3", question.title), el("p", question.text));
  if (question.link) {
    const a = el("a", "Apri la proposta");
    a.href = question.link;
    a.target = "_blank";
    a.rel = "noopener noreferrer";
    card.append(a);
  }
  const options = el("div", undefined, "decision-options");
  for (const choice of question.options) {
    const b = el("button", choice);
    b.type = "button";
    b.dataset.choice = choice;
    b.setAttribute("aria-pressed", "false");
    b.addEventListener("click", () => {
      const value = decision(question.id);
      value.choice = value.choice === choice ? "" : choice;
      for (const peer of options.querySelectorAll("button"))
        peer.setAttribute(
          "aria-pressed",
          String(peer.dataset.choice === value.choice),
        );
      changed();
    });
    options.append(b);
  }
  card.append(options);
  const label = el("label", "Indicazioni sulla proposta"),
    comment = el("textarea");
  comment.rows = 3;
  comment.addEventListener("input", () => {
    decision(question.id).comment = comment.value;
    changed();
  });
  label.append(comment);
  card.append(label);
  document.querySelector("#decision-list").append(card);
}
// File picker and drag-and-drop share validation and preserve the original bytes.
async function attachFiles(card, files) {
  const input = card.querySelector(".images");
  if (!loaded || input.disabled || !files.length) return;
  const initialDraft = draft, initialEntry = attachmentEntry(card);
  input.disabled = true;
  try {
    const usedBefore = usedAttachmentBytes();
    if (usedBefore + files.reduce((sum, file) => sum + file.size, 0) > maxTotal ||
        initialEntry.images.length + files.length > 30)
      throw Error("Massimo 30 allegati per riquadro e 20 MB complessivi.");
    const additions = await Promise.all(files.map(async (file) => {
      let type = file.type;
      if (!type || type === "application/octet-stream") {
        if (/\.svg$/i.test(file.name)) type = "image/svg+xml";
        if (/\.zip$/i.test(file.name)) type = "application/zip";
      }
      if (type === "application/x-zip-compressed") type = "application/zip";
      if (!allowedMime.includes(type) || file.size < 1 || file.size > maxFile)
        throw Error("Usa PNG, JPG, WebP, GIF, SVG o ZIP fino a 8 MB ciascuno e 20 MB complessivi.");
      if (type === "image/svg+xml") {
        const document = new DOMParser().parseFromString(await file.text(), "image/svg+xml");
        if (document.querySelector("parsererror") || document.documentElement.localName !== "svg" ||
            document.documentElement.namespaceURI !== "http://www.w3.org/2000/svg")
          throw Error("Il file SVG non è valido.");
      }
      if (type === "application/zip") {
        const signature = new Uint8Array(await file.slice(0, 4).arrayBuffer());
        if (signature.length !== 4 || signature[0] !== 80 || signature[1] !== 75 ||
            ![[3, 4], [5, 6], [7, 8]].some(([a, b]) => signature[2] === a && signature[3] === b))
          throw Error("Il file ZIP non è riconosciuto.");
      }
      return new Promise((resolve, reject) => {
        const reader = new FileReader();
        reader.onload = () => resolve({name: file.name, type, size: file.size,
          data: "data:" + type + ";base64," + reader.result.split(",")[1]});
        reader.onerror = () => reject(Error("Impossibile leggere il file."));
        reader.readAsDataURL(file);
      });
    }));
    if (draft !== initialDraft || attachmentEntry(card) !== initialEntry)
      throw Error("Le risposte sono state sostituite: allega nuovamente i file.");
    const used = usedAttachmentBytes();
    if (used + additions.reduce((sum, image) => sum + image.size, 0) > maxTotal ||
        initialEntry.images.length + additions.length > 30)
      throw Error("Massimo 30 allegati per riquadro e 20 MB complessivi.");
    initialEntry.images.push(...additions);
    drawAttachments(card);
    changed();
    report("Allegati aggiunti interi.");
  } catch (error) {
    report(error.message, true);
  } finally {
    input.disabled = false;
    input.value = "";
  }
}
// Keep file drops outside a response from replacing the document in this tab.
for (const name of ["dragover", "drop"]) document.addEventListener(name, (event) => {
  if (Array.from(event.dataTransfer.types).includes("Files")) event.preventDefault();
});
for (const card of document.querySelectorAll(".test")) {
  for (const b of card.querySelectorAll(".outcome"))
    b.addEventListener("click", () => {
      const value = entry(card.dataset.id);
      value.status = value.status === b.dataset.status ? "" : b.dataset.status;
      for (const peer of card.querySelectorAll(".outcome"))
        peer.setAttribute(
          "aria-pressed",
          String(peer.dataset.status === value.status),
        );
      card.querySelector(".item-state").textContent =
        value.status || "Non provato";
      changed();
    });
  card.querySelector(".comment").addEventListener("input", (event) => {
    entry(card.dataset.id).comment = event.target.value;
    changed();
  });
}
for (const card of document.querySelectorAll(".test, .extra")) {
  const input = card.querySelector(".images");
  input.addEventListener("change", () => {
    attachFiles(card, Array.from(input.files));
  });
  let dragDepth = 0;
  card.addEventListener("dragenter", (event) => {
    if (!Array.from(event.dataTransfer.types).includes("Files")) return;
    event.preventDefault();
    dragDepth++;
    card.classList.add("drop-active");
  });
  card.addEventListener("dragover", (event) => {
    if (!Array.from(event.dataTransfer.types).includes("Files")) return;
    event.preventDefault();
    event.dataTransfer.dropEffect = "copy";
  });
  card.addEventListener("dragleave", () => {
    if (--dragDepth <= 0) {
      dragDepth = 0;
      card.classList.remove("drop-active");
    }
  });
  card.addEventListener("drop", (event) => {
    if (!Array.from(event.dataTransfer.types).includes("Files")) return;
    event.preventDefault();
    dragDepth = 0;
    card.classList.remove("drop-active");
    attachFiles(card, Array.from(event.dataTransfer.files));
  });
}
for (const key of ["device", "tablet", "installed", "notes"])
  document.querySelector("#" + key).addEventListener("input", (event) => {
    draft[key] = event.target.value;
    changed();
  });
// Anchor the next card below the actual sticky dashboard, including wrapped mobile text.
const responseCards = Array.from(document.querySelectorAll(".test, .decision, .extra"));
const previousCard = document.querySelector("#previous-card");
const nextCard = document.querySelector("#next-card");
const firstEmpty = document.querySelector("#first-empty");
function navigationOffset() {
  return document.querySelector(".dashboard").getBoundingClientRect().height + 12;
}
function currentCardIndex() {
  const offset = navigationOffset();
  let index = -1;
  for (let i = 0; i < responseCards.length; i++) {
    if (responseCards[i].getBoundingClientRect().top <= offset + 2) index = i;
    else break;
  }
  // A section heading between cards belongs to the upcoming visible card.
  if (index >= 0 && index < responseCards.length - 1 &&
      responseCards[index].getBoundingClientRect().bottom < offset) index++;
  return index;
}
function firstEmptyCard() {
  return responseCards.find(card => !card.classList.contains("extra") && !card.classList.contains("has-response"));
}
function refreshNavigation() {
  const index = currentCardIndex();
  previousCard.disabled = !loaded || index <= 0;
  nextCard.disabled = !loaded || index >= responseCards.length - 1;
  const empty = firstEmptyCard();
  firstEmpty.hidden = !empty || responseCards[index] === empty;
  firstEmpty.disabled = !loaded;
  document.documentElement.style.setProperty("--feedback-scroll-offset", navigationOffset() + "px");
}
function goToCard(card) {
  if (!card) return;
  window.scrollTo({top: window.scrollY + card.getBoundingClientRect().top - navigationOffset(), behavior: "instant"});
  document.querySelector("#navigation-position").textContent =
    card.querySelector(".check-position")?.textContent || card.querySelector("h3,h2").textContent;
  refreshNavigation();
}
previousCard.addEventListener("click", () => goToCard(responseCards[currentCardIndex() - 1]));
nextCard.addEventListener("click", () => goToCard(responseCards[currentCardIndex() + 1]));
firstEmpty.addEventListener("click", () => goToCard(firstEmptyCard()));
let navigationFrame = null;
window.addEventListener("scroll", () => {
  if (navigationFrame !== null) return;
  navigationFrame = requestAnimationFrame(() => {
    navigationFrame = null;
    refreshNavigation();
  });
}, {passive: true});
new ResizeObserver(refreshNavigation).observe(document.querySelector(".dashboard"));
for (const id of ["save", "floating-save"])
  document.querySelector("#" + id).addEventListener("click", save);
document.querySelector("#copy").addEventListener("click", copy);
document.querySelector("#send").addEventListener("click", async () => {
  draft.completed = new Date().toISOString();
  revision++;
  if (!await save()) {
    report("Invio non confermato: le ultime modifiche non sono ancora disponibili all'agente. Riprova quando il salvataggio cloud funziona.", true);
    return;
  }
  document.querySelector("#summary").value = summary();
  report(
    remote
      ? "Giro reso leggibile all'agente. Puoi modificarlo e inviarlo di nuovo; l'agente lo leggerà solo dopo il tuo via in chat."
      : "Riepilogo pronto. Per renderlo leggibile dal cloud, importa il JSON nel documento cloud e premi Invia.",
  );
  document
    .querySelector("#summary")
    .scrollIntoView({ behavior: "smooth", block: "center" });
});
document.querySelector("#export").addEventListener("click", () => {
  const blob = new Blob(
      [JSON.stringify({ ...draft, version: spec.version }, null, 2)],
      { type: "application/json" },
    ),
    url = URL.createObjectURL(blob),
    a = el("a");
  a.href = url;
  a.download = `AIV-feedback-${spec.version}.json`;
  a.click();
  setTimeout(() => URL.revokeObjectURL(url), 1000);
  report("JSON esportato, con risposte e allegati.");
});
document.querySelector("#import").addEventListener("change", async (event) => {
  const file = event.target.files[0];
  if (!file) return;
  try {
    if (file.size > 35 * 1024 * 1024) throw Error("Il JSON supera 35 MB.");
    const imported = validate(JSON.parse(await file.text()));
    draft = imported;
    revision++;
    hydrate();
    await save();
    report("JSON importato. Le risposte sono state ripristinate.");
    document.querySelector("#summary").value = "";
  } catch (error) {
    report("Importazione annullata: " + error.message, true);
  } finally {
    event.target.value = "";
  }
});
document.querySelector("#reset").addEventListener("click", async () => {
  if (
    !confirm(
      remote ? "Azzera la bozza cloud, rimuovendo risposte e allegati da tutti i dispositivi? Esporta il JSON per conservarli." : "Cancellare tutte le risposte e tutti gli allegati salvati in questo browser? Esporta il JSON per conservarle.",
    )
  )
    return;
  draft = blank();
  revision++;
  hydrate();
  document.querySelector("#summary").value = "";
  await save();
  report("Risposte del browser azzerate.");
});
document.addEventListener("keydown", (event) => {
  if ((event.ctrlKey || event.metaKey) && event.key.toLowerCase() === "s") {
    event.preventDefault();
    save();
  }
});
document.addEventListener("visibilitychange", () => {
  if (document.visibilityState === "hidden" && saveTimer !== null) save();
});
function controls(disabled) {
  for (const control of document.querySelectorAll("button,input,textarea"))
    control.disabled = disabled;
  window.feedbackFormatting?.setDisabled(disabled);
}
controls(true);
(async () => {
  try {
    if (remote) {
      const existing = await remote.load(validate);
      if (existing) draft = existing;
      remoteReady = true;
      remote.account(true);
      saved.textContent = existing ? "Risposte ripristinate dal cloud." : "Nessuna risposta nel cloud: puoi iniziare.";
    } else {
      db = await new Promise((resolve, reject) => {
        const request = indexedDB.open("aiv-feedback", 1);
        request.onupgradeneeded = () =>
          request.result.createObjectStore("drafts");
        request.onsuccess = () => resolve(request.result);
        request.onerror = () => reject(request.error);
        request.onblocked = () =>
          reject(Error("Chiudi le altre schede del documento."));
      });
      db.onversionchange = () => {
        db.close();
        db = null;
        saved.textContent = "Memoria chiusa da un'altra scheda: esporta il JSON.";
      };
      const existing = await new Promise((resolve, reject) => {
        const request = db
          .transaction("drafts")
          .objectStore("drafts")
          .get("current");
        request.onsuccess = () => resolve(request.result);
        request.onerror = () => reject(request.error);
      });
      if (existing) draft = validate(existing);
      saved.textContent = draft.updated
        ? "Risposte ripristinate dal browser."
        : "Nessuna risposta salvata: puoi iniziare.";
    }
  } catch (error) {
    remote?.failed(error);
    const needsLogin = remote && error.status === 401;
    saved.textContent = needsLogin
      ? "Accedi con GitHub per compilare il documento e importare il JSON."
      : (remote ? "Cloud non disponibile. " : "Memoria non disponibile o dati non leggibili. Esporta il JSON prima di chiudere. ") + error.message;
    saved.classList.toggle("error", !needsLogin);
  } finally {
    loaded = remote ? remoteReady : true;
    alignDocumentVersion();
    hydrate();
    controls(!loaded);
    refreshNavigation();
  }
})();

window.feedbackHasUnsaved = () => revision !== persistedRevision;
window.feedbackSaveForLogout = async () => await save() && revision === persistedRevision;
async function refreshRemote() {
  if (!remote || !remoteReady || syncing || pendingSaves || revision !== persistedRevision || document.hidden) return;
  const observed = revision;
  syncing = true;
  let locked = false;
  try {
    if (!await remote.hasUpdates() || observed !== revision || pendingSaves) return;
    controls(true);
    locked = true;
    const existing = await remote.load(validate);
    draft = existing || blank();
    alignDocumentVersion();
    revision++;
    persistedRevision = revision;
    hydrate();
    document.querySelector("#summary").value = "";
    saved.textContent = "Risposte aggiornate dal cloud.";
    saved.classList.remove("error");
  } catch (error) {
    remote.failed(error);
    saved.textContent = "Sincronizzazione non riuscita. " + error.message;
    saved.classList.add("error");
  } finally {
    if (locked) { controls(false); refreshNavigation(); }
    syncing = false;
  }
}
if (remote) {
  setInterval(refreshRemote,15000);
  window.addEventListener("focus",refreshRemote);
  window.addEventListener("online",() => {
    if (remoteReady && revision !== persistedRevision && !pendingSaves) save();
    else refreshRemote();
  });
  document.addEventListener("visibilitychange",() => { if (!document.hidden) refreshRemote(); });
  window.addEventListener("beforeunload",event => {
    if (revision !== persistedRevision) { event.preventDefault(); event.returnValue = ""; }
  });
}
