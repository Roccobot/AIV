"use strict";
const spec = JSON.parse(document.querySelector("#feedback-data").textContent);
const outcomes = ["Tutto OK", "Accettabile", "Non approvato"];
const maxImage = 8 * 1024 * 1024,
  maxTotal = 20 * 1024 * 1024;
const allowedMime = ["image/png", "image/jpeg", "image/webp", "image/gif", "image/svg+xml"];
const blank = () => ({
  schema: 1,
  project: "AIV",
  version: spec.version,
  installed: spec.version,
  device: "",
  notes: "",
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
  saveTimer = null;
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
    const images = value.images.map((img) => {
      if (
        !img ||
        typeof img.name !== "string" ||
        img.name.length > 500 ||
        !allowedMime.includes(img.type) ||
        typeof img.data !== "string" ||
        !img.data.startsWith("data:" + img.type + ";base64,") ||
        !Number.isInteger(img.size) ||
        img.size < 1 ||
        img.size > maxImage
      )
        throw Error("Immagine non valida.");
      const encoded = img.data.split(",")[1];
      if (
        !/^[A-Za-z0-9+/]*={0,2}$/.test(encoded) ||
        encoded.length % 4 !== 0 ||
        encoded.length > Math.ceil(maxImage / 3) * 4
      )
        throw Error("Dati immagine non validi.");
      const bytes = atob(encoded).length;
      if (bytes !== img.size) throw Error("Dimensione immagine non valida.");
      total += bytes;
      if (total > maxTotal) throw Error("Le immagini superano 20 MB.");
      return { name: img.name, type: img.type, size: img.size, data: img.data };
    });
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
  for (const card of document.querySelectorAll(".test")) {
    const value = entry(card.dataset.id);
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
}
function hydrate() {
  for (const card of document.querySelectorAll(".test")) {
    const value = entry(card.dataset.id);
    card.querySelector(".comment").value = value.comment;
    card.querySelector(".item-state").textContent =
      value.status || "Non provato";
    for (const b of card.querySelectorAll(".outcome"))
      b.setAttribute("aria-pressed", String(b.dataset.status === value.status));
    drawImages(card);
  }
  for (const card of document.querySelectorAll(".decision")) {
    const value = decision(card.dataset.id);
    card.querySelector("textarea").value = value.comment;
    for (const b of card.querySelectorAll("button"))
      b.setAttribute("aria-pressed", String(b.dataset.choice === value.choice));
  }
  for (const key of ["device", "installed", "notes"])
    document.querySelector("#" + key).value = draft[key];
  refreshCounts();
}
function drawImages(card) {
  const list = card.querySelector(".image-list");
  list.replaceChildren();
  entry(card.dataset.id).images.forEach((img, index) => {
    const figure = el("figure"),
      image = el("img");
    image.src = img.data;
    image.alt = img.name;
    figure.append(image, el("figcaption", img.name));
    const remove = el("button", "Rimuovi immagine");
    remove.type = "button";
    remove.addEventListener("click", () => {
      entry(card.dataset.id).images.splice(index, 1);
      drawImages(card);
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
  const current = revision,
    snapshot = structuredClone(draft);
  snapshot.updated = new Date().toISOString();
  snapshot.version = spec.version;
  saved.textContent = "Salvataggio in corso...";
  saved.classList.remove("error");
  saveQueue = saveQueue
    .catch(() => {})
    .then(
      () =>
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
    .then(() => {
      if (current === revision) {
        draft.updated = snapshot.updated;
        saved.textContent =
          "Salvato in questo browser: " +
          new Date(snapshot.updated).toLocaleString("it-IT");
      }
      return true;
    })
    .catch((error) => {
      saved.textContent =
        "Non salvato: esporta il JSON prima di chiudere. " + error.message;
      saved.classList.add("error");
      return false;
    });
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
    `Dispositivo: ${draft.device || "Non indicato"}`,
    "",
  ];
  for (const item of spec.items) {
    const value = entry(item.id);
    lines.push(`${item.id} - ${item.title}: ${value.status || "Non provato"}`);
    if (value.comment) lines.push(value.comment);
    if (value.images.length)
      lines.push(
        `Immagini: ${value.images.map((i) => i.name).join(", ")} (consegnare con JSON o allegati)`,
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
  return lines.join("\n");
}
async function copy() {
  const text = summary();
  document.querySelector("#summary").value = text;
  try {
    await navigator.clipboard.writeText(text);
    report("Riepilogo copiato. Incollalo in chat con le eventuali immagini.");
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
  const initialDraft = draft, initialEntry = entry(card.dataset.id);
  input.disabled = true;
  try {
    const usedBefore = Object.values(draft.entries).reduce((sum, e) =>
      sum + e.images.reduce((size, image) => size + image.size, 0), 0);
    if (usedBefore + files.reduce((sum, file) => sum + file.size, 0) > maxTotal ||
        initialEntry.images.length + files.length > 30)
      throw Error("Massimo 30 immagini per voce e 20 MB complessivi.");
    const additions = await Promise.all(files.map(async (file) => {
      const type = /\.svg$/i.test(file.name) && (!file.type || file.type === "application/octet-stream")
        ? "image/svg+xml" : file.type;
      if (!allowedMime.includes(type) || file.size < 1 || file.size > maxImage)
        throw Error("Usa PNG, JPG, WebP, GIF o SVG fino a 8 MB ciascuno e 20 MB complessivi.");
      if (type === "image/svg+xml") {
        const document = new DOMParser().parseFromString(await file.text(), "image/svg+xml");
        if (document.querySelector("parsererror") || document.documentElement.localName !== "svg" ||
            document.documentElement.namespaceURI !== "http://www.w3.org/2000/svg")
          throw Error("Il file SVG non è valido.");
      }
      return new Promise((resolve, reject) => {
        const reader = new FileReader();
        reader.onload = () => resolve({name: file.name, type, size: file.size,
          data: "data:" + type + ";base64," + reader.result.split(",")[1]});
        reader.onerror = () => reject(Error("Impossibile leggere l'immagine."));
        reader.readAsDataURL(file);
      });
    }));
    if (draft !== initialDraft || entry(card.dataset.id) !== initialEntry)
      throw Error("Le risposte sono state sostituite: allega nuovamente le immagini.");
    const used = Object.values(draft.entries).reduce((sum, e) =>
      sum + e.images.reduce((size, image) => size + image.size, 0), 0);
    if (used + additions.reduce((sum, image) => sum + image.size, 0) > maxTotal ||
        initialEntry.images.length + additions.length > 30)
      throw Error("Massimo 30 immagini per voce e 20 MB complessivi.");
    initialEntry.images.push(...additions);
    drawImages(card);
    changed();
    report("Immagini aggiunte intere.");
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
for (const key of ["device", "installed", "notes"])
  document.querySelector("#" + key).addEventListener("input", (event) => {
    draft[key] = event.target.value;
    changed();
  });
for (const id of ["save", "floating-save"])
  document.querySelector("#" + id).addEventListener("click", save);
document.querySelector("#copy").addEventListener("click", copy);
document.querySelector("#send").addEventListener("click", async () => {
  draft.completed = new Date().toISOString();
  revision++;
  await save();
  document.querySelector("#summary").value = summary();
  report(
    "Riepilogo pronto. Copialo e invialo in chat: nulla è stato spedito automaticamente.",
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
  report("JSON esportato, con risposte e immagini.");
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
      "Cancellare tutte le risposte e immagini salvate in questo browser? Esporta il JSON per conservarle.",
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
}
controls(true);
(async () => {
  try {
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
  } catch (error) {
    saved.textContent =
      "Memoria non disponibile o dati non leggibili. Esporta il JSON prima di chiudere. " +
      error.message;
    saved.classList.add("error");
  } finally {
    loaded = true;
    hydrate();
    controls(false);
  }
})();
