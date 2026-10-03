"use strict";
/* Feedback document, interface. Second of the four page scripts (see feedback-data.js).
   Theme, messages, counters, cards and attachments, the Altro fields and overlay, and the
   six Consegna commands. Uses the data and save() from feedback-data.js; refreshCounts()
   calls refreshNavigation() from feedback-nav.js at run time. */
/* DF theme: on load it follows the system; T (no modifiers, outside the fields)
   switches light and dark for this session only. Nothing is stored, so a reload
   goes back to the system theme. */
const toggleTheme = (function initFeedbackTheme() {
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
  return function toggleTheme() {
    override = effective() === "dark" ? "light" : "dark";
    apply();
  };
})();
// Messages of the commands, in the strip under the save status.
const message = document.querySelector("#action-message");
function el(tag, text, cls) {
  const e = document.createElement(tag);
  if (text !== undefined) e.textContent = text;
  if (cls) e.className = cls;
  return e;
}
function attachmentEntry(card) {
  return card.classList.contains("extra") ? draft.extra : entry(card.dataset.id);
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
    card.dataset.outcomeKind = spec.outcomes.find((outcome) => outcome.label === value.status)?.kind || "";
    card.classList.toggle("has-response", Boolean(value.status || value.comment.trim() || value.images.length));
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
    ...spec.outcomes.map((outcome) => countChip(outcome.kind, outcome.label, counters[outcome.label])),
  );
  document.querySelector("#progress").value = done;
  document.querySelector("#answered").textContent =
    `Risposte: ${done} su ${spec.items.length}.`;
  refreshNavigation();
}

function hydrate() {
  for (const card of document.querySelectorAll(".test")) {
    const value = entry(card.dataset.id);
    card.querySelector(".comment").value = value.comment;
    for (const b of card.querySelectorAll(".outcome"))
      b.setAttribute("aria-pressed", String(b.dataset.status === value.status));
    drawAttachments(card);
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
function changed() {
  revision++;
  draft.completed = null;
  refreshCounts();
  clearTimeout(saveTimer);
  saveTimer = setTimeout(save, 350);
  saved.textContent = "Modifiche da salvare...";
}
// Copy a text to the clipboard, with the selection fallback for browsers that refuse the API.
async function copyText(text) {
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
      if (!document.execCommand("copy")) throw Error("Copia non riuscita.");
    } finally {
      area.remove();
    }
  }
}
async function copy() {
  try {
    await copyText(summary());
    report("Riepilogo copiato. Incollalo in chat con gli eventuali allegati.");
  } catch {
    report("Il browser non ha permesso la copia negli appunti: esporta il JSON.", true);
  }
}
function copyLabelId(id) {
  copyText(String(id || "").toLowerCase()).catch(() => {});
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

// --- Mobile Altro overlay (long press on the floating ⇥); same draft.notes as the page Altro ---
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

// --- Consegna commands: one row under Altro, copied into the mobile Altro overlay ---
// The page row keeps the ids (#save, #copy...); the copy is found through data-command.
const pageCommands = document.querySelector("#extra-section .altro-commands");
const overlayCommands = pageCommands.cloneNode(true);
for (const node of overlayCommands.querySelectorAll("[id]")) node.removeAttribute("id");
// Under the panel, not in its body: feedback-format.js rewraps the body around the field.
document.querySelector(".altro-overlay-panel").append(overlayCommands);

async function send() {
  draft.completed = new Date().toISOString();
  revision++;
  if (!await save()) {
    report("Invio non confermato: le ultime modifiche non sono ancora disponibili all'agente. Riprova quando il salvataggio cloud funziona.", true);
    return;
  }
  report(
    remote
      ? "Giro reso leggibile all'agente. Puoi modificarlo e inviarlo di nuovo; l'agente lo leggerà solo dopo il tuo via in chat."
      : "Risposte pronte. Per renderle leggibili dal cloud, importa il JSON nel documento cloud e premi Invia.",
  );
}
function exportJson() {
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
}
async function importJson(input) {
  const file = input.files[0];
  if (!file) return;
  try {
    if (file.size > 35 * 1024 * 1024) throw Error("Il JSON supera 35 MB.");
    const imported = validate(JSON.parse(await file.text()));
    draft = imported;
    revision++;
    hydrate();
    await save();
    report("JSON importato. Le risposte sono state ripristinate.");
  } catch (error) {
    report("Importazione annullata: " + error.message, true);
  } finally {
    input.value = "";
  }
}
async function reset() {
  if (
    !confirm(
      remote ? "Azzera la bozza cloud, rimuovendo risposte e allegati da tutti i dispositivi? Esporta il JSON per conservarli." : "Cancellare tutte le risposte e tutti gli allegati salvati in questo browser? Esporta il JSON per conservarle.",
    )
  )
    return;
  draft = blank();
  revision++;
  hydrate();
  await save();
  report(remote ? "Bozza cloud azzerata." : "Risposte del browser azzerate.");
}
const commands = { reset, copy, export: exportJson, save, send };
for (const row of [pageCommands, overlayCommands]) {
  for (const button of row.querySelectorAll("button[data-command]"))
    button.addEventListener("click", () => commands[button.dataset.command]());
  const picker = row.querySelector('[data-command="import"] input');
  picker.addEventListener("change", () => importJson(picker));
}
// The page's only keyboard handler: Ctrl/Cmd+S saves, Escape closes the Altro overlay, and
// T switches the theme outside the fields. The format shortcuts belong to each editor.
function typing(target) {
  if (!(target instanceof Element)) return false;
  if (["INPUT", "TEXTAREA", "SELECT"].includes(target.tagName)) return true;
  return Boolean(target.closest("[contenteditable]")?.isContentEditable);
}
document.addEventListener("keydown", (event) => {
  const modified = event.ctrlKey || event.metaKey || event.altKey || event.shiftKey;
  if ((event.ctrlKey || event.metaKey) && event.key.toLowerCase() === "s") {
    event.preventDefault();
    save();
  } else if (event.key === "Escape" && altroOverlay && !altroOverlay.hidden) {
    setAltroOverlayOpen(false);
  } else if (event.key.toLowerCase() === "t" && !modified && !typing(event.target)) {
    event.preventDefault();
    toggleTheme();
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
