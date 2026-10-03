"use strict";
/* Tema DF: al carico segue il sistema; T (senza modificatori, fuori dai campi)
   scambia chiaro↔scuro solo per la sessione. Nessun localStorage/cookie; il
   refresh torna al sistema. */
(function initFeedbackTheme() {
  let override = null;
  const mq = window.matchMedia("(prefers-color-scheme: dark)");
  function effective() {
    return override || (mq.matches ? "dark" : "light");
  }
  function apply() {
    document.documentElement.setAttribute("data-theme", effective());
  }
  function onSystemChange() {
    if (override === null) apply();
  }
  if (mq.addEventListener) mq.addEventListener("change", onSystemChange);
  else if (mq.addListener) mq.addListener(onSystemChange);
  apply();
  document.addEventListener("keydown", (event) => {
    if (event.key.toLowerCase() !== "t") return;
    if (event.ctrlKey || event.metaKey || event.altKey || event.shiftKey) return;
    const target = event.target instanceof Element ? event.target : null;
    if (target) {
      const tag = target.tagName;
      if (tag === "INPUT" || tag === "TEXTAREA" || tag === "SELECT") return;
      if (target.isContentEditable) return;
      const host = target.closest("[contenteditable]");
      if (host && host.isContentEditable) return;
    }
    event.preventDefault();
    override = effective() === "dark" ? "light" : "dark";
    apply();
  });
})();
const spec = JSON.parse(document.querySelector("#feedback-data").textContent);
if (!Array.isArray(spec.labels)) spec.labels = [];
const outcomes = ["Tutto OK", "Accettabile", "Non approvato"];
const maxFile = 8 * 1024 * 1024,
  maxTotal = 20 * 1024 * 1024;
const allowedMime = ["image/png", "image/jpeg", "image/webp", "image/gif", "image/svg+xml", "application/zip"];
// Preserve the historical images key in JSON drafts; it also holds ZIP files.
const blank = () => ({
  schema: 1,
  project: "AIV",
  version: spec.version,
  installed: "",
  device: "",
  tablet: "",
  notes: "",
  extra: { images: [] },
  entries: {},
  decisions: {},
  labels: {},
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
function labelEntry(id) {
  return draft.labels[id] ?? (draft.labels[id] = { revision: "" });
}
// Empty revision keeps the proposal. A cleared field is stored as a one-character sentinel.
const LABEL_CLEARED = "\u0001";
function labelById(id) {
  return (spec.labels || []).find((item) => item.id === id);
}
function labelShown(item) {
  const revision = labelEntry(item.id).revision;
  if (revision === LABEL_CLEARED) return "";
  if (revision === "") return item.proposal;
  return revision;
}
function labelDirty(item) {
  return labelShown(item) !== item.proposal;
}
function writeLabel(item, text) {
  const value = labelEntry(item.id);
  if (text === item.proposal) value.revision = "";
  else if (text === "") value.revision = LABEL_CLEARED;
  else value.revision = text;
}
function strokeIcon(paths) {
  const svg = document.createElementNS("http://www.w3.org/2000/svg", "svg");
  svg.setAttribute("viewBox", "0 0 24 24");
  svg.setAttribute("fill", "none");
  svg.setAttribute("stroke", "currentColor");
  svg.setAttribute("stroke-width", "2");
  svg.setAttribute("stroke-linecap", "round");
  svg.setAttribute("stroke-linejoin", "round");
  for (const d of paths) {
    const path = document.createElementNS("http://www.w3.org/2000/svg", "path");
    path.setAttribute("d", d);
    svg.append(path);
  }
  return svg;
}
function labelA11yIcon() {
  const svg = strokeIcon([
    "M12 21a9 9 0 1 0 0-18 9 9 0 0 0 0 18z",
    "M12 8.2a1.15 1.15 0 1 0 0-2.3 1.15 1.15 0 0 0 0 2.3z",
    "M8.2 11.2h7.6",
    "M12 11.2v3.1",
    "M12 14.3 9.6 18.4",
    "M12 14.3l2.4 4.1",
  ]);
  svg.setAttribute("class", "label-a11y");
  svg.setAttribute("role", "img");
  svg.setAttribute("aria-label", "Solo lettore di schermo");
  return svg;
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
    Array.isArray(raw.decisions) ||
    (raw.labels !== undefined &&
      (typeof raw.labels !== "object" || Array.isArray(raw.labels)))
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
    Object.keys(raw.decisions).length > 100 ||
    Object.keys(raw.labels || {}).length > 200
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
  for (const [id, value] of Object.entries(raw.labels || {})) {
    if (
      !/^e-[A-Za-z0-9._-]+$/.test(id) ||
      !value ||
      typeof value.revision !== "string" ||
      value.revision.length > 100000
    )
      throw Error("Etichetta non valida.");
    clean.labels[id] = { revision: value.revision };
  }
  return clean;
}
function countIcon(kind) {
  const svg = document.createElementNS("http://www.w3.org/2000/svg", "svg");
  svg.setAttribute("viewBox", "0 0 24 24");
  svg.setAttribute("aria-hidden", "true");
  svg.setAttribute("class", "count-icon count-" + kind);
  const path = document.createElementNS("http://www.w3.org/2000/svg", "path");
  // Material-like: check / alert / cancel (no emoji).
  path.setAttribute("d", {
    ok: "M9.2 16.6 4.8 12.2l1.4-1.4 3 3 8-8 1.4 1.4z",
    warn: "M1 21h22L12 2 1 21zm12-3h-2v-2h2v2zm0-4h-2v-4h2v4z",
    bad: "M12 2a10 10 0 1 0 0 20 10 10 0 0 0 0-20zm3.5 13.1-1.4 1.4L12 13.4l-2.1 2.1-1.4-1.4L10.6 12 8.5 9.9l1.4-1.4L12 10.6l2.1-2.1 1.4 1.4L13.4 12l2.1 2.1z",
  }[kind]);
  svg.append(path);
  return svg;
}
function countChip(kind, label, value) {
  const chip = el("span", undefined, "count-chip count-chip-" + kind);
  chip.append(countIcon(kind), el("span", String(value)));
  chip.title = label + ": " + value;
  chip.setAttribute("aria-label", label + ": " + value);
  return chip;
}
function refreshCounts() {
  document.querySelector(".extra")?.classList.toggle("has-response", Boolean(draft.notes.trim() || draft.extra.images.length));
  for (const card of document.querySelectorAll(".test")) {
    const value = entry(card.dataset.id);
    card.dataset.outcome = value.status;
    card.classList.toggle("has-response", Boolean(value.status || value.comment.trim() || value.images.length));
  }
  for (const card of document.querySelectorAll(".decision")) {
    const value = decision(card.dataset.id);
    card.classList.toggle("has-response", Boolean(value.choice || value.comment.trim()));
  }
  for (const card of document.querySelectorAll(".label-card")) {
    const item = labelById(card.dataset.id);
    const dirty = item ? labelDirty(item) : false;
    card.classList.toggle("has-response", dirty);
    const dot = card.querySelector(".label-modified");
    if (dot) dot.hidden = !dirty;
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
  const ratio = el("span", `${done}/${spec.items.length}`, "count-ratio");
  ratio.id = "count-answered";
  ratio.setAttribute("aria-label", `Riscontri: ${done} su ${spec.items.length}`);
  counts.replaceChildren(
    ratio,
    countChip("ok", "Tutto OK", counters["Tutto OK"]),
    countChip("warn", "Accettabile", counters["Accettabile"]),
    countChip("bad", "Non approvato", counters["Non approvato"]),
  );
  document.querySelector("#progress").value = done;
  document.querySelector("#answered").textContent =
    `Risposte: ${done} su ${spec.items.length}.`;
  refreshNavigation();
}

function alignDocumentVersion() {
  // ⚠️ **Nuovo rilascio del documento: svuota i campi liberi** (giro 3.25-3.30 / 3.37 note A).
  // Telefono/tablet restano. Prove con ID ancora presenti restano.
  // Decisioni non più in spec.decisions non si ripropongono in UI (lista vuota = chiuse).
  // La conferma 'ho installato questa versione' si azzera: va rifatta sul nuovo giro.
  if (draft.version === spec.version) return;
  draft.notes = "";
  draft.extra = { images: [] };
  draft.completed = null;
  draft.version = spec.version;
  draft.installed = "";
  syncAltroFields();
}
function hydrate() {
  for (const card of document.querySelectorAll(".test")) {
    const value = entry(card.dataset.id);
    card.querySelector(".comment").value = value.comment;
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
  for (const card of document.querySelectorAll(".label-card")) {
    const item = labelById(card.dataset.id);
    if (!item) continue;
    card.querySelector("textarea").value = labelShown(item);
  }
  for (const key of ["device", "tablet", "notes"])
    document.querySelector("#" + key).value = draft[key];
  syncInstalledConfirm();
  syncAltroFields();
  drawAttachments(document.querySelector(".extra"));
  window.feedbackFormatting?.refresh();
  refreshCounts();
  const dec = document.querySelector("#decisions");
  if (dec) dec.hidden = !(spec.decisions && spec.decisions.length);
  const lab = document.querySelector("#labels");
  if (lab) lab.hidden = !(spec.labels && spec.labels.length);
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
        window.feedbackHoldEditingAfterSave?.();
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
    draft.installed === spec.version
      ? `Versione installata: ${spec.version} (confermata)`
      : `Versione installata: non confermata`,
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
  if (spec.labels && spec.labels.length) {
    lines.push("", "Etichette testuali");
    for (const item of spec.labels) {
      const text = labelShown(item);
      if (text.trim() === item.proposal.trim())
        lines.push(`${item.id}: (approvata)`);
      else {
        lines.push(`${item.id}: ${text.trim()}`);
        lines.push("Proposta era: " + item.proposal);
      }
    }
  }
  lines.push(
    "",
    "Altro",
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
async function copyLabelId(id) {
  const text = String(id || "").toLowerCase();
  try {
    await navigator.clipboard.writeText(text);
  } catch {
    const area = document.createElement("textarea");
    area.value = text;
    area.setAttribute("readonly", "");
    area.setAttribute("aria-hidden", "true");
    area.tabIndex = -1;
    area.style.position = "fixed";
    area.style.top = "0";
    area.style.left = "0";
    area.style.opacity = "0";
    document.body.append(area);
    area.focus();
    area.select();
    try {
      document.execCommand("copy");
    } finally {
      area.remove();
    }
  }
}
for (const item of spec.labels || []) {
  const card = el("article", undefined, "card label-card");
  card.dataset.id = item.id;
  const title = el("h3", item.title);
  const dot = el("span", undefined, "label-modified");
  dot.hidden = true;
  dot.setAttribute("aria-label", "modificato");
  title.append(dot);
  const idCopy = el("button", String(item.id).toLowerCase(), "label-id");
  idCopy.type = "button";
  idCopy.addEventListener("click", () => {
    copyLabelId(item.id);
  });
  card.append(idCopy, title);
  if (item.a11y) card.append(labelA11yIcon());
  const label = el("label", item.title, "sr-only");
  const field = el("textarea");
  field.className = "label-revision";
  field.rows = 3;
  field.value = item.proposal;
  field.addEventListener("input", () => {
    writeLabel(item, field.value);
    changed();
  });
  label.append(field);
  card.append(label);
  document.querySelector("#label-list").append(card);
}
window.feedbackRestoreLabel = (area) => {
  const card = area.closest(".label-card");
  const item = card && labelById(card.dataset.id);
  if (!item) return;
  if (!window.confirm("Vuoi tornare alla mia proposta originale?")) return;
  writeLabel(item, item.proposal);
  area.value = item.proposal;
  window.feedbackFormatting?.refresh();
  changed();
};
// File picker and drag-and-drop share validation and preserve the original bytes.
function imageInputs(card) {
  if (!card.classList.contains("extra")) return [card.querySelector(".images")].filter(Boolean);
  return Array.from(document.querySelectorAll("#altro-attach .images, #altro-overlay-attach .images"));
}
async function attachFiles(card, files) {
  const inputs = imageInputs(card);
  const input = inputs[0];
  if (!loaded || !input || inputs.some(item => item.disabled) || !files.length) return;
  const initialDraft = draft, initialEntry = attachmentEntry(card);
  inputs.forEach(item => { item.disabled = true; });
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
    inputs.forEach(item => {
      item.disabled = false;
      item.value = "";
    });
  }
}
async function decodeClipboardImage(source) {
  if (typeof createImageBitmap === "function") {
    try {
      return await createImageBitmap(source);
    } catch {
      // The Image fallback handles clipboard formats unsupported by createImageBitmap.
    }
  }
  return new Promise((resolve, reject) => {
    const url = URL.createObjectURL(source);
    const image = new Image();
    image.onload = () => {
      URL.revokeObjectURL(url);
      resolve(image);
    };
    image.onerror = () => {
      URL.revokeObjectURL(url);
      reject(Error("Impossibile leggere l'immagine dagli appunti."));
    };
    image.src = url;
  });
}
async function clipboardImageAsPng(event) {
  const data = event.clipboardData;
  if (!data) return null;
  const item = Array.from(data.items || []).find(candidate =>
    candidate.kind === "file" && /^image\//i.test(candidate.type));
  const source = item?.getAsFile?.() ||
    Array.from(data.files || []).find(file => /^image\//i.test(file.type));
  if (!source) return null;
  const image = await decodeClipboardImage(source);
  const width = image.width || image.naturalWidth;
  const height = image.height || image.naturalHeight;
  if (!width || !height) throw Error("L'immagine dagli appunti non ha dimensioni valide.");
  const canvas = document.createElement("canvas");
  canvas.width = width;
  canvas.height = height;
  const context = canvas.getContext("2d");
  if (!context) throw Error("Impossibile convertire l'immagine in PNG.");
  context.drawImage(image, 0, 0);
  image.close?.();
  const png = await new Promise(resolve => canvas.toBlob(resolve, "image/png"));
  if (!png) throw Error("Impossibile convertire l'immagine in PNG.");
  return new File([png], "clipboard.png", {type: "image/png", lastModified: Date.now()});
}
function attachmentCardForPaste(target) {
  const card = target.closest?.(".test, .extra");
  if (card) return card;
  if (target.id === "notes-mobile-editor" || target.id === "notes-mobile" ||
      target.closest?.("#altro-overlay"))
    return document.querySelector(".extra");
  return null;
}
window.feedbackPasteImage = (event, target) => {
  const card = attachmentCardForPaste(target);
  const data = event.clipboardData;
  const hasImage = data && (Array.from(data.items || []).some(item =>
    item.kind === "file" && /^image\//i.test(item.type)) ||
    Array.from(data.files || []).some(file => /^image\//i.test(file.type)));
  if (!card || !hasImage) return false;
  event.preventDefault();
  clipboardImageAsPng(event)
    .then(file => attachFiles(card, [file]))
    .catch(error => report(error.message, true));
  return true;
};
document.addEventListener("paste", event => {
  const target = event.target;
  if (target.closest?.(".rich-editor")) return;
  if (target.matches?.("textarea:not([readonly])"))
    window.feedbackPasteImage(event, target);
});
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
const altroOverlayImages = document.querySelector("#altro-overlay-attach .images");
if (altroOverlayImages) {
  altroOverlayImages.addEventListener("change", () => {
    const card = document.querySelector(".extra");
    if (card) attachFiles(card, Array.from(altroOverlayImages.files));
  });
}
for (const key of ["device", "tablet"])
  document.querySelector("#" + key).addEventListener("input", (event) => {
    draft[key] = event.target.value;
    changed();
  });

function syncInstalledConfirm() {
  const box = document.querySelector("#installed-confirm");
  const giro = document.querySelector("#giro-version");
  if (giro) giro.textContent = spec.version;
  if (box) box.checked = draft.installed === spec.version;
}
document.querySelector("#installed-confirm")?.addEventListener("change", (event) => {
  draft.installed = event.target.checked ? spec.version : "";
  changed();
});

let altroSyncing = false;
function syncAltroFields(source) {
  if (altroSyncing) return;
  altroSyncing = true;
  const value = draft.notes || "";
  for (const id of ["notes", "notes-mobile"]) {
    const node = document.querySelector("#" + id);
    if (!node || node === source) continue;
    if (node.value !== value) node.value = value;
  }
  // Keep rich-editor mirrors in sync when format.js has wrapped #notes.
  window.feedbackFormatting?.refresh?.();
  altroSyncing = false;
}
function onAltroInput(event) {
  draft.notes = event.target.value;
  syncAltroFields(event.target);
  changed();
}
for (const id of ["notes", "notes-mobile"]) {
  const node = document.querySelector("#" + id);
  if (node) node.addEventListener("input", onAltroInput);
}

// --- Mobile Altro overlay (long-press floating ⇥); same draft.notes as bottom Altro ---
const altroOverlay = document.querySelector("#altro-overlay");
function setAltroOverlayOpen(open) {
  if (!altroOverlay) return;
  altroOverlay.hidden = !open;
  document.body.classList.toggle("altro-overlay-open", open);
  if (open) {
    syncAltroFields();
    const editor =
      document.querySelector("#notes-mobile-editor") ||
      document.querySelector("#notes-mobile");
    editor?.focus?.();
  }
}
document.querySelectorAll(".altro-overlay-close").forEach((button) => {
  button.addEventListener("click", () => setAltroOverlayOpen(false));
});
altroOverlay?.addEventListener("click", (event) => {
  if (event.target === altroOverlay) setAltroOverlayOpen(false);
});
document.addEventListener("keydown", (event) => {
  if (event.key === "Escape" && altroOverlay && !altroOverlay.hidden)
    setAltroOverlayOpen(false);
});

// --- Consegna e copie overlay (Altro button or long-press Salva) ---
const deliveryOverlay = document.querySelector("#delivery-overlay");
const openDeliveryBtn = document.querySelector("#open-delivery");
function setDeliveryOverlayOpen(open) {
  if (!deliveryOverlay) return;
  deliveryOverlay.hidden = !open;
  document.documentElement.classList.toggle("delivery-overlay-open", open);
  document.body.classList.toggle("delivery-overlay-open", open);
  if (open) {
    const first = document.querySelector("#send") || document.querySelector("#save");
    first?.focus?.();
  }
}
function closeDeliveryAfterAction() {
  setDeliveryOverlayOpen(false);
}
openDeliveryBtn?.addEventListener("click", () => setDeliveryOverlayOpen(true));
document.querySelectorAll(".delivery-overlay-close").forEach((button) => {
  button.addEventListener("click", () => setDeliveryOverlayOpen(false));
});
deliveryOverlay?.addEventListener("click", (event) => {
  if (event.target === deliveryOverlay) setDeliveryOverlayOpen(false);
});
// A drag on the scrim, or on any strip the panel does not cover, must not scroll the page.
document.addEventListener(
  "touchmove",
  (event) => {
    if (!document.body.classList.contains("delivery-overlay-open")) return;
    const panel = deliveryOverlay?.querySelector(".delivery-overlay-panel");
    if (panel && panel.contains(event.target)) return;
    event.preventDefault();
  },
  { passive: false },
);
document.addEventListener("keydown", (event) => {
  if (event.key === "Escape" && deliveryOverlay && !deliveryOverlay.hidden)
    setDeliveryOverlayOpen(false);
});

// --- Mobile editing: only Salva while a text field is focused ---
const isMobileUi = () => window.matchMedia("(max-width: 720px)").matches;
let editingHoldTimer = null;
function setEditingMobile(on) {
  if (!isMobileUi()) {
    document.body.classList.remove("editing-mobile");
    return;
  }
  document.body.classList.toggle("editing-mobile", on);
}
function holdEditingAfterSave() {
  clearTimeout(editingHoldTimer);
  setEditingMobile(true);
  editingHoldTimer = setTimeout(() => {
    const active = document.activeElement;
    const still =
      active &&
      (active.matches("textarea, input:not([type=file]), .rich-editor") ||
        active.closest?.(".rich-editor"));
    if (!still) setEditingMobile(false);
  }, 5000);
}
document.addEventListener(
  "focusin",
  (event) => {
    const t = event.target;
    if (!t) return;
    if (
      t.matches?.("textarea:not([readonly]), input:not([type=file]):not([readonly]), .rich-editor") ||
      t.closest?.(".rich-editor")
    )
      setEditingMobile(true);
  },
  true,
);
document.addEventListener(
  "focusout",
  () => {
    clearTimeout(editingHoldTimer);
    editingHoldTimer = setTimeout(() => {
      const active = document.activeElement;
      const still =
        active &&
        (active.matches?.("textarea:not([readonly]), input:not([type=file]):not([readonly]), .rich-editor") ||
          active.closest?.(".rich-editor"));
      if (!still) setEditingMobile(false);
    }, 0);
  },
  true,
);
window.feedbackHoldEditingAfterSave = holdEditingAfterSave;
// Anchor the next card below the actual sticky dashboard, including wrapped mobile text.
const responseCards = Array.from(document.querySelectorAll(".test, .decision, .extra"));
const previousCard = document.querySelector("#previous-card");
const nextCard = document.querySelector("#next-card");
const firstEmpty = document.querySelector("#first-empty");
const dashboard = document.querySelector(".dashboard");
function refreshDashboardDocked() {
  // Sticky positioning is not exposed as a CSS state. The viewport edge is the
  // reliable boundary between the normal floating strip and its docked state.
  dashboard.classList.toggle("is-docked", dashboard.getBoundingClientRect().top <= 0);
}
function navigationOffset() {
  return document.querySelector(".dashboard").getBoundingClientRect().height + 12;
}
function currentCardIndex() {
  const cards = responseCards;
  const offset = navigationOffset();
  const tops = cards.map((card) => card.getBoundingClientRect().top);
  let index = -1;
  for (let i = 0; i < cards.length; i++) {
    if (tops[i] <= offset + 2) index = i;
    else break;
  }
  // A section heading between cards belongs to the upcoming visible card.
  if (index >= 0 && index < cards.length - 1 &&
      cards[index].getBoundingClientRect().bottom < offset) index++;
  // Sticky Altro sits beside the proofs. If a proof is actually at the
  // offset, that proof is current. Altro stays current only past the proofs.
  if (index >= 0 && cards[index].classList.contains("extra")) {
    let proof = -1;
    let best = -Infinity;
    for (let i = 0; i < index; i++) {
      if (cards[i].classList.contains("extra")) continue;
      if (tops[i] <= offset + 2 && tops[i] > best) {
        best = tops[i];
        proof = i;
      }
    }
    if (proof >= 0 && tops[proof] > offset - 80) index = proof;
  }
  return index;
}
function firstEmptyCard() {
  return responseCards.find(card => !card.classList.contains("extra") && !card.classList.contains("has-response"));
}
function refreshNavigation() {
  refreshDashboardDocked();
  const index = currentCardIndex();
  previousCard.disabled = !loaded || index <= 0;
  nextCard.disabled = !loaded || index >= responseCards.length - 1;
  const empty = firstEmptyCard();
  // On mobile keep ⇥ visible so long-press can open Altro even when nothing is empty.
  const diskFab = document.querySelector("#floating-save");
  if (isMobileUi()) {
    firstEmpty.hidden = false;
    firstEmpty.title = empty
      ? "Primo riquadro non compilato · tieni premuto per Altro"
      : "Tieni premuto per Altro";
    firstEmpty.setAttribute(
      "aria-label",
      empty
        ? "Primo riquadro non compilato. Tieni premuto per aprire Altro"
        : "Tieni premuto per aprire Altro",
    );
    if (diskFab) {
      diskFab.title = "Salva · tieni premuto per Consegna e copie";
      diskFab.setAttribute(
        "aria-label",
        "Salva le risposte. Tieni premuto per aprire Consegna e copie",
      );
    }
  } else {
    firstEmpty.hidden = !empty || responseCards[index] === empty;
    firstEmpty.title = "Primo riquadro non compilato";
    firstEmpty.setAttribute("aria-label", "Primo riquadro non compilato");
    if (diskFab) {
      diskFab.title = "Salva · tieni premuto per Consegna e copie";
      diskFab.setAttribute(
        "aria-label",
        "Salva le risposte. Tieni premuto per aprire Consegna e copie",
      );
    }
  }
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
let firstEmptyLongPress = false;
let firstEmptyLongTimer = null;
function clearFirstEmptyLongPress() {
  clearTimeout(firstEmptyLongTimer);
  firstEmptyLongTimer = null;
}
firstEmpty.addEventListener("pointerdown", (event) => {
  if (!isMobileUi() || event.button != null && event.button !== 0) return;
  firstEmptyLongPress = false;
  clearFirstEmptyLongPress();
  firstEmptyLongTimer = setTimeout(() => {
    firstEmptyLongPress = true;
    setAltroOverlayOpen(true);
  }, 450);
});
firstEmpty.addEventListener("pointerup", clearFirstEmptyLongPress);
firstEmpty.addEventListener("pointercancel", clearFirstEmptyLongPress);
firstEmpty.addEventListener("pointerleave", clearFirstEmptyLongPress);
firstEmpty.addEventListener("click", (event) => {
  if (firstEmptyLongPress) {
    event.preventDefault();
    event.stopImmediatePropagation();
    firstEmptyLongPress = false;
    return;
  }
  const empty = firstEmptyCard();
  if (empty) goToCard(empty);
  else if (isMobileUi()) {
    const target = document.querySelector("#extra-section");
    target?.scrollIntoView({ behavior: "smooth", block: "start" });
  }
});
let navigationFrame = null;
window.addEventListener("scroll", () => {
  if (navigationFrame !== null) return;
  navigationFrame = requestAnimationFrame(() => {
    navigationFrame = null;
    refreshNavigation();
  });
}, {passive: true});
new ResizeObserver(refreshNavigation).observe(document.querySelector(".dashboard"));
document.querySelector("#save").addEventListener("click", () => {
  save();
});
const floatingSave = document.querySelector("#floating-save");
let floatingSaveLongPress = false;
let floatingSaveLongTimer = null;
function clearFloatingSaveLongPress() {
  clearTimeout(floatingSaveLongTimer);
  floatingSaveLongTimer = null;
}
floatingSave.addEventListener("pointerdown", (event) => {
  if (event.button != null && event.button !== 0) return;
  floatingSaveLongPress = false;
  clearFloatingSaveLongPress();
  floatingSaveLongTimer = setTimeout(() => {
    floatingSaveLongPress = true;
    setDeliveryOverlayOpen(true);
  }, 450);
});
floatingSave.addEventListener("pointerup", clearFloatingSaveLongPress);
floatingSave.addEventListener("pointercancel", clearFloatingSaveLongPress);
floatingSave.addEventListener("pointerleave", clearFloatingSaveLongPress);
floatingSave.addEventListener("click", (event) => {
  if (floatingSaveLongPress) {
    event.preventDefault();
    event.stopImmediatePropagation();
    floatingSaveLongPress = false;
    return;
  }
  save();
});
document.querySelector("#copy").addEventListener("click", async () => {
  await copy();
  closeDeliveryAfterAction();
});
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
  closeDeliveryAfterAction();
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
  closeDeliveryAfterAction();
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
    closeDeliveryAfterAction();
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
  closeDeliveryAfterAction();
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
