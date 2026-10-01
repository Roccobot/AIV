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
      if (text[index] === "\\" && /[\\*\[\]]/.test(text[index + 1] || "")) {
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
        let end = index + marker.length;
        while (end < text.length) {
          if (text[end] === "\\") { end += 2; continue; }
          if (text.startsWith(marker, end)) break;
          end++;
        }
        if (end >= text.length) continue;
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
  function markdown(root) {
    const runs = [];
    const add = (text, style) => {
      if (!text) return;
      const last = runs.at(-1);
      if (last && last.bold === style.bold && last.italic === style.italic && last.href === style.href) last.text += text;
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
      let text = run.text.replace(/[\\*\[\]]/g, "\\$&");
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
    const toolbar = node("div", undefined, "format-toolbar");
    toolbar.setAttribute("role", "group");
    toolbar.setAttribute("aria-label", "Formattazione del testo");
    const box = node("div", undefined, "rich-editor");
    box.id = area.id + "-editor";
    box.contentEditable = String(!area.disabled);
    box.setAttribute("role", "textbox");
    box.setAttribute("aria-multiline", "true");
    box.setAttribute("aria-labelledby", label.id);
    box.setAttribute("aria-disabled", String(area.disabled));
    box.dataset.placeholder = area.placeholder;
    box.spellcheck = true;
    label.htmlFor = box.id;
    label.addEventListener("click", () => box.focus());
    area.hidden = true;
    area.setAttribute("aria-hidden", "true");
    area.tabIndex = -1;
    wrapper.append(toolbar, area, box);
    const editor = {area, box, range: null, lastMarkdown: null};
    editors.push(editor);
    for (const [kind, title, key] of [["bold", "Grassetto", "B"], ["italic", "Corsivo", "I"], ["link", "Link", "K"]]) {
      const button = node("button", title);
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
    box.addEventListener("beforeinput", event => {
      // Both Enter variants use blocks, avoiding a browser-only terminal newline placeholder.
      if (event.inputType === "insertLineBreak") {
        event.preventDefault();
        document.execCommand("insertParagraph", false);
      }
    });
    box.addEventListener("input", () => { links(editor); sync(editor); });
    box.addEventListener("paste", event => {
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
      const kind = {b: "bold", i: "italic", k: "link"}[event.key.toLowerCase()];
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
  window.feedbackFormatting = {
    refresh: () => editors.forEach(render),
    setDisabled: disabled => editors.forEach(editor => {
      editor.box.contentEditable = String(!disabled);
      editor.box.setAttribute("aria-disabled", String(disabled));
    })
  };
})();
