"use strict";
(() => {
  const editors = [];
  const iconPaths = {
    // Bold: the Arial Bold "B" the button used to set in text (22px, 0.6px stroke), as a
    // path in the 24px box, so it no longer depends on the fonts installed. Glyph from
    // Liberation Sans Bold, metric-compatible with Arial; its ink matched the text version.
    bold: "M18.945 14.182Q18.945 16.244 17.398 17.372Q15.851 18.5 13.101 18.5H5.528V3.364H12.457Q15.228 3.364 16.651 4.326Q18.075 5.287 18.075 7.167Q18.075 8.456 17.36 9.342Q16.646 10.229 15.185 10.54Q17.022 10.755 17.983 11.695Q18.945 12.635 18.945 14.182ZM14.884 7.597Q14.884 6.576 14.234 6.146Q13.584 5.717 12.306 5.717H8.697V9.466H12.328Q13.67 9.466 14.277 8.999Q14.884 8.531 14.884 7.597ZM15.765 13.935Q15.765 11.808 12.714 11.808H8.697V16.147H12.833Q14.358 16.147 15.062 15.594Q15.765 15.041 15.765 13.935Z",
    // Italic, link and code keep Material-style paths.
    italic: "M10 4v3h2.21l-3.42 10H6v3h8v-3h-2.21l3.42-10H18V4z",
    link: "M3.9 12c0-1.71 1.39-3.1 3.1-3.1h4V7H7a5 5 0 0 0 0 10h4v-1.9H7A3.1 3.1 0 0 1 3.9 12zM8 13h8v-2H8v2zm9-6h-4v1.9h4a3.1 3.1 0 0 1 0 6.2h-4V17h4a5 5 0 0 0 0-10z",
    code: "M8.7 16.7 3.9 12l4.8-4.7L7.3 5.9 1.2 12l6.1 6.1 1.4-1.4zm6.6 0 1.4 1.4L22.8 12l-6.1-6.1-1.4 1.4 4.8 4.7-4.8 4.7z",
  };
  function resetIcon() {
    const svg = document.createElementNS("http://www.w3.org/2000/svg", "svg");
    svg.setAttribute("viewBox", "0 0 24 24");
    svg.setAttribute("aria-hidden", "true");
    svg.setAttribute("focusable", "false");
    svg.setAttribute("fill", "none");
    svg.setAttribute("stroke", "currentColor");
    svg.setAttribute("stroke-width", "2");
    svg.setAttribute("stroke-linecap", "round");
    svg.setAttribute("stroke-linejoin", "round");
    const path = document.createElementNS("http://www.w3.org/2000/svg", "path");
    // Small circular arrow. Drawn here; not taken from an external reset file.
    path.setAttribute("d", "M20 12a8 8 0 1 1-2.2-5.5M20 4.5V9h-4.5");
    path.setAttribute("fill", "none");
    svg.append(path);
    return svg;
  }
  function formatIcon(kind) {
    const svg = document.createElementNS("http://www.w3.org/2000/svg", "svg");
    svg.setAttribute("viewBox", "0 0 24 24");
    svg.setAttribute("aria-hidden", "true");
    svg.setAttribute("focusable", "false");
    const path = document.createElementNS("http://www.w3.org/2000/svg", "path");
    path.setAttribute("d", iconPaths[kind]);
    if (kind === "bold") {
      // A thin stroke of its own colour gives the letter the weight of the other glyphs.
      path.setAttribute("stroke", "currentColor");
      path.setAttribute("stroke-width", "0.6");
      path.setAttribute("paint-order", "stroke fill");
    }
    svg.append(path);
    return svg;
  }
  function node(tag, text, className) {
    const element = document.createElement(tag);
    if (text !== undefined) element.textContent = text;
    if (className) element.className = className;
    return element;
  }
  function strokeIcon(paths, width = 2) {
    const svg = document.createElementNS("http://www.w3.org/2000/svg", "svg");
    svg.setAttribute("viewBox", "0 0 24 24");
    svg.setAttribute("aria-hidden", "true");
    svg.setAttribute("focusable", "false");
    svg.setAttribute("fill", "none");
    svg.setAttribute("stroke", "currentColor");
    svg.setAttribute("stroke-width", String(width));
    svg.setAttribute("stroke-linecap", "round");
    svg.setAttribute("stroke-linejoin", "round");
    for (const d of paths) {
      const path = document.createElementNS("http://www.w3.org/2000/svg", "path");
      path.setAttribute("d", d);
      svg.append(path);
    }
    return svg;
  }
  // Mobile Altro: two rows of six keys under the field, both always visible (the user's request,
  // 2026-10-06, which drops the two states of the mockup Altro_mobile and their switch keys):
  // Allega, the four formats and Chiudi, then the six commands. The commands are the copy
  // feedback-ui.js puts under the panel: it moves into these rows.
  function overlayRow(actions) {
    const close = node("button", undefined, "altro-row-key altro-row-close");
    close.type = "button";
    close.id = "altro-overlay-close";
    close.setAttribute("aria-label", "Chiudi");
    close.title = "Chiudi";
    close.append(strokeIcon(["M6 6l12 12", "M18 6 6 18"]));
    close.addEventListener("pointerdown", event => event.preventDefault());
    close.addEventListener("click", () => window.feedbackCloseAltro?.());
    actions.append(close);
    const commands = document.querySelector(".altro-overlay-panel > .altro-commands");
    if (commands) actions.append(commands);
  }
  function webAddress(value) {
    try {
      const address = new URL(value);
      return ["http:", "https:"].includes(address.protocol) ? address.href : null;
    } catch {
      return null;
    }
  }
  // Parse inline emphasis, code, and web links; user text never becomes HTML.
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
      if (text[index] === "\\" && /[\\*`\[\]_]/.test(text[index + 1] || "")) {
        plain += text[index + 1];
        index += 2;
        continue;
      }
      if (text[index] === "`") {
        const close = text.indexOf("`", index + 1);
        if (close > index) {
          flush();
          parent.append(node("code", text.slice(index + 1, close)));
          index = close + 1;
          continue;
        }
      }
      const link = /^\[((?:\\.|[^\]\\])+)\]\((https?:\/\/[^\s)]+)\)/.exec(text.slice(index));
      const address = link && webAddress(link[2]);
      if (address) {
        flush();
        const anchor = node("a");
        anchor.href = address;
        anchor.target = "_blank";
        anchor.rel = "noopener noreferrer";
        inline(link[1].replace(/\\([\\*`\[\]_])/g, "$1"), anchor, depth + 1);
        parent.append(anchor);
        index += link[0].length;
        continue;
      }
      let matched = false;
      for (const marker of ["***", "**", "*", "_"]) {
        if (!text.startsWith(marker, index)) continue;
        let end = index + marker.length;
        while (end < text.length) {
          if (text[end] === "\\") { end += 2; continue; }
          if (text.startsWith(marker, end)) break;
          end++;
        }
        if (end >= text.length) continue;
        if (end <= index + marker.length) continue;
        flush();
        const emphasis = node(marker === "*" || marker === "_" ? "em" : "strong");
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

  function markdown(root) {
    const runs = [];
    const add = (text, style) => {
      if (!text) return;
      const last = runs.at(-1);
      if (last && last.bold === style.bold && last.italic === style.italic && last.href === style.href && last.code === style.code) last.text += text;
      else runs.push({...style, text});
    };
    function visit(element, style) {
      if (element.nodeType === Node.TEXT_NODE) {
        add(element.textContent, style);
        return;
      }
      if (element.nodeType !== Node.ELEMENT_NODE) return;
      const tag = element.tagName;
      if (tag === "BR") {
        // Shift+Enter leaves a terminal BR to hold the caret on the new line.
        if (!element.nextSibling && element.previousSibling?.nodeName === "BR") return;
        add("\n", style);
        return;
      }
      const block = ["DIV", "P"].includes(tag);
      if (block && element.previousSibling) add("\n", {});
      // ⚠️ A run of its own, written back as it is: escaped with the rest, its backticks
      // became \` and the code came back as plain text (found by the check, 2026-10-04).
      if (tag === "CODE") {
        add("`" + element.textContent + "`", {...style, code: true});
        return;
      }
      const next = {
        bold: element.style.fontWeight ? /^(bold|[6-9]00)$/.test(element.style.fontWeight) : style.bold || ["B", "STRONG"].includes(tag),
        italic: element.style.fontStyle ? element.style.fontStyle === "italic" : style.italic || ["I", "EM"].includes(tag),
        href: tag === "A" ? webAddress(element.getAttribute("href")) : style.href
      };
      // An empty block's sole BR is a caret placeholder, not an extra line.
      if (!(block && element.childNodes.length === 1 && element.firstChild.nodeName === "BR")) {
        for (const child of element.childNodes) visit(child, next);
      }
    }
    if (!(root.childNodes.length === 1 && root.firstChild.nodeName === "BR")) {
      for (const child of root.childNodes) visit(child, {});
    }
    return runs.map(run => {
      if (run.code) return run.text;
      let text = run.text.replace(/[\\*`\[\]_]/g, "\\$&");
      const marker = (run.bold ? "**" : "") + (run.italic ? "*" : "");
      if (marker) text = text.replace(/^(\s*)([\s\S]*?\S)(\s*)$/, (_, before, body, after) => before + marker + body + marker + after);
      if (run.href) text = "[" + text + "](" + run.href.replace(/\(/g, "%28").replace(/\)/g, "%29") + ")";
      return text;
    }).join("");
  }
  function render(editor) {
    if (editor.area.value === editor.lastMarkdown) return;
    editor.box.replaceChildren();
    inline(editor.area.value, editor.box);
    editor.lastMarkdown = editor.area.value;
    editor.range = null;
  }
  function inside(editor, range) {
    return editor.box.contains(range.startContainer) && editor.box.contains(range.endContainer);
  }
  function selection(editor) {
    const selected = window.getSelection();
    if (selected.rangeCount && inside(editor, selected.getRangeAt(0))) return selected.getRangeAt(0);
    if (editor.range && inside(editor, editor.range)) {
      selected.removeAllRanges();
      selected.addRange(editor.range);
      return editor.range;
    }
    const range = document.createRange();
    range.selectNodeContents(editor.box);
    range.collapse(false);
    selected.removeAllRanges();
    selected.addRange(range);
    return range;
  }
  function sync(editor) {
    // The original textarea remains the bridge to persistence and schema-1 exports.
    editor.area.value = markdown(editor.box);
    editor.lastMarkdown = editor.area.value;
    editor.area.dispatchEvent(new Event("input", {bubbles: true}));
    refreshNavigation();
  }
  function links(editor) {
    for (const anchor of editor.box.querySelectorAll("a")) {
      anchor.target = "_blank";
      anchor.rel = "noopener noreferrer";
    }
  }
  function edit(editor, kind) {
    if (editor.area.disabled) return;
    editor.box.focus({preventScroll: true});
    const range = selection(editor);
    if (kind === "link") {
      const selected = range.toString();
      const container = range.startContainer.nodeType === Node.ELEMENT_NODE ? range.startContainer : range.startContainer.parentElement;
      const anchor = container.closest("a");
      const proposed = prompt("Indirizzo del link (http:// o https://):", anchor?.href || webAddress(selected) || "https://");
      if (proposed === null) return;
      const address = webAddress(proposed.trim());
      if (!address) {
        report("Inserisci un indirizzo http:// o https:// valido.", true);
        return;
      }
      if (range.collapsed && !anchor) {
        document.execCommand("insertText", false, "testo del link");
        const caret = window.getSelection().getRangeAt(0);
        const label = document.createRange();
        label.setStart(caret.endContainer, caret.endOffset - "testo del link".length);
        label.setEnd(caret.endContainer, caret.endOffset);
        window.getSelection().removeAllRanges();
        window.getSelection().addRange(label);
      }
      document.execCommand("createLink", false, address);
      links(editor);
    } else if (kind === "code") {
      // Rendered in the editor like bold, italic and links (the user's request, 2026-10-04):
      // a <code> node, which markdown() writes back between backticks. Inside one, the key
      // takes the code away again.
      const container = range.startContainer.nodeType === Node.ELEMENT_NODE ? range.startContainer : range.startContainer.parentElement;
      const current = container.closest("code");
      if (current && editor.box.contains(current)) {
        current.replaceWith(document.createTextNode(current.textContent));
      } else {
        const code = node("code", range.toString() || "codice");
        range.deleteContents();
        range.insertNode(code);
        // The caret goes after the code, in a text node of its own, so what follows is plain.
        const after = document.createTextNode("");
        code.after(after);
        const caret = document.createRange();
        caret.setStart(after, 0);
        caret.collapse(true);
        window.getSelection().removeAllRanges();
        window.getSelection().addRange(caret);
      }
    } else document.execCommand(kind === "bold" ? "bold" : "italic", false);
    sync(editor);
  }
  for (const [index, area] of Array.from(document.querySelectorAll("textarea:not([readonly])")).entries()) {
    const label = area.parentElement;
    const wrapper = node("div", undefined, "formatted-field");
    label.replaceWith(wrapper);
    wrapper.append(label);
    area.id ||= "formatted-comment-" + index;
    label.id = area.id + "-label";
    const actions = node("div", undefined, "format-actions");
    const toolbar = node("div", undefined, "format-toolbar");
    toolbar.setAttribute("role", "group");
    toolbar.setAttribute("aria-label", "Formattazione del testo");
    const box = node("div", undefined, "rich-editor");
    box.id = area.id + "-editor";
    box.contentEditable = String(!area.disabled);
    box.setAttribute("role", "textbox");
    box.setAttribute("aria-multiline", "true");
    const named = area.getAttribute("aria-label");
    if (named) box.setAttribute("aria-label", named);
    else box.setAttribute("aria-labelledby", label.id);
    box.setAttribute("aria-disabled", String(area.disabled));
    box.dataset.placeholder = area.placeholder;
    box.spellcheck = true;
    label.htmlFor = box.id;
    label.addEventListener("click", () => box.focus());
    area.hidden = true;
    area.setAttribute("aria-hidden", "true");
    area.tabIndex = -1;
    if (area.classList.contains("label-revision")) {
      const reset = node("button");
      reset.type = "button";
      reset.className = "label-reset";
      reset.setAttribute("aria-label", "Ripristina la proposta originale");
      reset.append(resetIcon());
      reset.addEventListener("pointerdown", event => event.preventDefault());
      reset.addEventListener("click", () => window.feedbackRestoreLabel?.(area));
      actions.append(reset);
    }
    actions.append(toolbar);
    wrapper.append(area, box, actions);
    const editor = {area, box, range: null, lastMarkdown: null};
    editors.push(editor);
    for (const [kind, title, key] of [["bold", "Grassetto", "B"], ["italic", "Corsivo", "I"], ["code", "Codice", "M"], ["link", "Link", "K"]]) {
      const button = node("button");
      button.setAttribute("aria-label", title);
      button.append(formatIcon(kind));
      button.type = "button";
      button.dataset.format = kind;
      button.title = title + " (⌘" + key + " / Ctrl+" + key + ")";
      button.setAttribute("aria-keyshortcuts", "Meta+" + key + " Control+" + key);
      button.disabled = area.disabled;
      // Preserve the selection on both mouse and touch before the toolbar takes focus.
      button.addEventListener("pointerdown", event => event.preventDefault());
      button.addEventListener("click", () => edit(editor, kind));
      toolbar.append(button);
    }
    // Altro only: compact attach sits immediately left of this field's format toolbar.
    if (area.id === "notes" || area.id === "notes-mobile") {
      const attach = document.getElementById(area.id === "notes" ? "altro-attach" : "altro-overlay-attach");
      if (attach) {
        const cluster = node("div", undefined, "altro-format-cluster altro-halves");
        toolbar.replaceWith(cluster);
        cluster.append(attach, toolbar);
      }
      if (area.id === "notes-mobile") overlayRow(actions);
    } else {
      // Proof cards: label.attachment follows this field (or sits on the card).
      // Not #altro-attach, and not label cards (they have no such label).
      const sibling = wrapper.nextElementSibling;
      const card = wrapper.closest("article.test");
      const onCard = card ? card.querySelector(":scope > label.attachment") : null;
      const attach = sibling && sibling.matches("label.attachment") ? sibling : onCard;
      if (attach && attach.id !== "altro-attach" && !attach.classList.contains("altro-attach")) {
        const cluster = node("div", undefined, "altro-format-cluster");
        toolbar.replaceWith(cluster);
        cluster.append(attach, toolbar);
      }
    }
    box.addEventListener("beforeinput", event => {
      // Both Enter variants use blocks, avoiding a browser-only terminal newline placeholder.
      if (event.inputType === "insertLineBreak") {
        event.preventDefault();
        document.execCommand("insertParagraph", false);
      }
    });
    box.addEventListener("input", () => { links(editor); sync(editor); });
    box.addEventListener("paste", event => {
      if (window.feedbackPasteImage?.(event, box)) return;
      event.preventDefault();
      // Plain-text paste prevents foreign HTML, styles and active elements entering the editor.
      document.execCommand("insertText", false, event.clipboardData.getData("text/plain"));
    });
    box.addEventListener("drop", event => {
      // File drops continue to the card's existing attachment handler.
      if (event.dataTransfer.files.length) event.preventDefault();
      else {
        event.preventDefault();
        box.focus();
        document.execCommand("insertText", false, event.dataTransfer.getData("text/plain"));
      }
    });
    box.addEventListener("click", event => {
      const anchor = event.target.closest("a");
      if (anchor && webAddress(anchor.href)) {
        event.preventDefault();
        window.open(anchor.href, "_blank", "noopener,noreferrer");
      }
    });
    box.addEventListener("keydown", event => {
      const kind = {b: "bold", i: "italic", m: "code", k: "link"}[event.key.toLowerCase()];
      if (kind && (event.metaKey || event.ctrlKey) && !event.altKey && !event.shiftKey) {
        event.preventDefault();
        edit(editor, kind);
      }
    });
    area.addEventListener("input", () => render(editor));
    render(editor);
  }
  document.addEventListener("selectionchange", () => {
    const selected = window.getSelection();
    if (!selected.rangeCount) return;
    const range = selected.getRangeAt(0);
    for (const editor of editors) {
      if (!inside(editor, range)) continue;
      editor.range = range.cloneRange();
      for (const kind of ["bold", "italic"]) {
        editor.box.parentElement.querySelector('[data-format="' + kind + '"]').setAttribute("aria-pressed", String(document.queryCommandState(kind)));
      }
    }
  });
  // The editor being written in: the one that has the focus, and only if it can be edited.
  function writing() {
    return editors.find(editor => editor.box === document.activeElement && !editor.area.disabled) || null;
  }
  window.feedbackFormatting = {
    refresh: () => editors.forEach(render),
    // Whether a field is being written in now: an attachment click then writes its name there.
    writing: () => Boolean(writing()),
    /* Inserts plain text at the caret of the field being written in, as typing would (the
       browser's own insertion, so undo works and the input event saves it). False when no
       field has the focus. */
    insertAtCaret: text => {
      const editor = writing();
      if (!editor) return false;
      selection(editor);
      document.execCommand("insertText", false, text);
      return true;
    },
    setDisabled: disabled => editors.forEach(editor => {
      editor.box.contentEditable = String(!disabled);
      editor.box.setAttribute("aria-disabled", String(disabled));
    })
  };
})();
