"use strict";
(() => {
  const editors = [];
  function node(tag, text, className) {
    const element = document.createElement(tag);
    if (text !== undefined) element.textContent = text;
    if (className) element.className = className;
    return element;
  }
  function webAddress(value) {
    try {
      const address = new URL(value);
      return ["http:", "https:"].includes(address.protocol) ? address.href : null;
    } catch {
      return null;
    }
  }
  // Parse only inline emphasis and web links; user text never becomes HTML.
  function inline(text, parent, depth = 0) {
    if (depth > 12) {
      parent.append(document.createTextNode(text));
      return;
    }
    let plain = "";
    const flush = () => {
      if (plain) parent.append(document.createTextNode(plain));
      plain = "";
    };
    for (let index = 0; index < text.length;) {
      if (text[index] === "\\" && index + 1 < text.length) {
        plain += text[index + 1];
        index += 2;
        continue;
      }
      const link = /^\[((?:\\.|[^\]\\])+)\]\((https?:\/\/[^\s)]+)\)/.exec(text.slice(index));
      const address = link && webAddress(link[2]);
      if (address) {
        flush();
        const anchor = node("a");
        anchor.href = address;
        anchor.target = "_blank";
        anchor.rel = "noopener noreferrer";
        inline(link[1], anchor, depth + 1);
        parent.append(anchor);
        index += link[0].length;
        continue;
      }
      let matched = false;
      for (const marker of ["***", "**", "*"]) {
        if (!text.startsWith(marker, index)) continue;
        const end = text.indexOf(marker, index + marker.length);
        if (end <= index + marker.length) continue;
        flush();
        const emphasis = node(marker === "*" ? "em" : "strong");
        if (marker === "***") {
          const italic = node("em");
          inline(text.slice(index + 3, end), italic, depth + 1);
          emphasis.append(italic);
        } else inline(text.slice(index + marker.length, end), emphasis, depth + 1);
        parent.append(emphasis);
        index = end + marker.length;
        matched = true;
        break;
      }
      if (!matched) plain += text[index++];
    }
    flush();
  }
  function render(editor) {
    editor.preview.replaceChildren();
    editor.preview.hidden = !/\*|\[[^\]]+\]\(/.test(editor.area.value);
    if (editor.preview.hidden) return;
    editor.preview.append(node("p", "Anteprima", "preview-caption"));
    const content = node("div", undefined, "formatted-content");
    inline(editor.area.value, content);
    editor.preview.append(content);
  }
  function edit(area, kind) {
    const start = area.selectionStart, end = area.selectionEnd;
    const selected = area.value.slice(start, end);
    let from = start, to = end, replacement, selectedFrom, selectedTo;
    if (kind === "link") {
      const proposed = prompt("Indirizzo del link (http:// o https://):", webAddress(selected) || "https://");
      if (proposed === null) return;
      const address = webAddress(proposed.trim());
      if (!address) {
        report("Inserisci un indirizzo http:// o https:// valido.", true);
        area.focus({preventScroll: true});
        return;
      }
      const label = selected || "testo del link";
      const safeLabel = label.replace(/[\\[\]]/g, "\\$&");
      replacement = "[" + safeLabel + "](" + address.replace(/\(/g, "%28").replace(/\)/g, "%29") + ")";
      selectedFrom = start + 1;
      selectedTo = selectedFrom + safeLabel.length;
    } else {
      const marker = kind === "bold" ? "**" : "*";
      const before = /\*+$/.exec(area.value.slice(0, start))?.[0].length || 0;
      const after = /^\*+/.exec(area.value.slice(end))?.[0].length || 0;
      const surrounds = kind === "bold" ? before >= 2 && after >= 2 : before % 2 === 1 && after % 2 === 1;
      if (surrounds && selected) {
        from -= marker.length;
        to += marker.length;
        replacement = selected;
        selectedFrom = from;
        selectedTo = from + selected.length;
      } else if (selected.startsWith(marker) && selected.endsWith(marker) && selected.length > 2 * marker.length) {
        replacement = selected.slice(marker.length, -marker.length);
        selectedFrom = start;
        selectedTo = start + replacement.length;
      } else {
        const text = selected || "testo";
        replacement = marker + text + marker;
        selectedFrom = start + marker.length;
        selectedTo = selectedFrom + text.length;
      }
    }
    area.focus({preventScroll: true});
    area.setSelectionRange(from, to);
    // insertText retains the native textarea undo history; fall back when unavailable.
    if (!document.execCommand("insertText", false, replacement)) {
      area.setRangeText(replacement, from, to, "end");
      area.dispatchEvent(new Event("input", {bubbles: true}));
    }
    area.setSelectionRange(selectedFrom, selectedTo);
  }
  for (const [index, area] of Array.from(document.querySelectorAll("textarea:not([readonly])")).entries()) {
    const label = area.parentElement;
    const wrapper = node("div", undefined, "formatted-field");
    label.replaceWith(wrapper);
    wrapper.append(label);
    area.id ||= "formatted-comment-" + index;
    label.htmlFor = area.id;
    const toolbar = node("div", undefined, "format-toolbar");
    toolbar.setAttribute("role", "group");
    toolbar.setAttribute("aria-label", "Formattazione del testo");
    for (const [kind, title, key] of [["bold", "Grassetto", "B"], ["italic", "Corsivo", "I"], ["link", "Link", "K"]]) {
      const button = node("button", title);
      button.type = "button";
      button.dataset.format = kind;
      button.title = title + " (⌘" + key + " / Ctrl+" + key + ")";
      button.setAttribute("aria-keyshortcuts", "Meta+" + key + " Control+" + key);
      button.disabled = area.disabled;
      button.addEventListener("mousedown", event => event.preventDefault());
      button.addEventListener("click", () => edit(area, kind));
      toolbar.append(button);
    }
    const preview = node("div", undefined, "markdown-preview");
    preview.setAttribute("aria-label", "Anteprima del testo formattato");
    wrapper.append(toolbar, area, preview);
    const editor = {area, preview};
    editors.push(editor);
    area.addEventListener("input", () => {
      render(editor);
      refreshNavigation();
    });
    area.addEventListener("keydown", event => {
      const kind = {b: "bold", i: "italic", k: "link"}[event.key.toLowerCase()];
      if (kind && (event.metaKey || event.ctrlKey) && !event.altKey && !event.shiftKey) {
        event.preventDefault();
        edit(area, kind);
      }
    });
    render(editor);
  }
  window.feedbackFormatting = {refresh: () => editors.forEach(render)};
})();
