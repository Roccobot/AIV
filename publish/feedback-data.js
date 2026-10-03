"use strict";
/* Feedback document, data and saving. Loaded first of the four page scripts, which share
   one global scope in this order: feedback-data.js, feedback-ui.js, feedback-nav.js,
   feedback-start.js. Holds the page data (spec), the draft and its state, JSON
   validation, the text summary and the save queue (cloud or IndexedDB). It only calls
   the other files at run time, never while loading: alignDocumentVersion() uses
   syncAltroFields() from feedback-ui.js. */
const spec = JSON.parse(document.querySelector("#feedback-data").textContent);
if (!Array.isArray(spec.labels)) spec.labels = [];
const outcomes = ["Tutto OK", "Accettabile", "Non approvato"];
const maxFile = 8 * 1024 * 1024,
  maxTotal = 20 * 1024 * 1024;
const allowedMime = ["image/png", "image/jpeg", "image/webp", "image/gif", "image/svg+xml", "application/zip"];
// Preserve the historical images key in JSON drafts; it also holds ZIP files.
// `decisions` stays in schema 1: the page no longer asks any, but old drafts carry them,
// the Worker requires the key, and an import must give them back unchanged.
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
// The save status line in the strip.
const saved = document.querySelector("#saved");
function entry(id) {
  return (
    draft.entries[id] ??
    (draft.entries[id] = { status: "", comment: "", images: [] })
  );
}
function usedAttachmentBytes() {
  return [draft.extra, ...Object.values(draft.entries)].reduce((sum, value) =>
    sum + value.images.reduce((bytes, file) => bytes + file.size, 0), 0);
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
function alignDocumentVersion() {
  // A new release of the document clears the free fields (rounds 3.25-3.30, 3.37 note A).
  // Phone and tablet stay, and so do answers to tests whose identifier is still listed.
  // The 'I installed this version' confirmation resets: it belongs to the new round.
  if (draft.version === spec.version) return;
  draft.notes = "";
  draft.extra = { images: [] };
  draft.completed = null;
  draft.version = spec.version;
  draft.installed = "";
  syncAltroFields();
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
