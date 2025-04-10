import {
  SELF_CLOSING_TAGS,
  VDocType,
  VDocumentFragment,
  VElement,
  VHTMLDocument,
  VNode,
  VTextNode,
  document,
  escapeHTML,
  hArgumentParser,
  hasOwn,
  isVElement,
  markup,
  unescapeHTML
} from "./chunk-YMRUYF4I.js";

// src/htmlparser.ts
var attrRe = /([^=\s]+)(\s*=\s*(("([^"]*)")|('([^']*)')|[^>\s]+))?/g;
var endTagRe = /^<\/([^>\s]+)[^>]*>/m;
var startTagRe = /^<([^>\s/]+)((\s+[^=>\s]+(\s*=\s*(("[^"]*")|('[^']*')|[^>\s]+))?)*)\s*(?:\/\s*)?>/m;
var selfCloseTagRe = /\s*\/\s*>\s*$/m;
var HtmlParser = class {
  constructor(options = {}) {
    this.attrRe = attrRe;
    this.endTagRe = endTagRe;
    this.startTagRe = startTagRe;
    this.defaults = { ignoreWhitespaceText: false };
    if (options.scanner)
      this.scanner = options.scanner;
    this.options = Object.assign({}, this.defaults, options);
  }
  parse(html2) {
    let treatAsChars = false;
    let index, match, characters;
    while (html2.length) {
      if (html2.substring(0, 4) === "<!--") {
        index = html2.indexOf("-->");
        if (index !== -1) {
          this.scanner.comment(html2.substring(4, index));
          html2 = html2.substring(index + 3);
          treatAsChars = false;
        } else {
          treatAsChars = true;
        }
      } else if (html2.substring(0, 2) === "</") {
        match = this.endTagRe.exec(html2);
        if (match) {
          html2 = RegExp.rightContext;
          treatAsChars = false;
          this.parseEndTag(RegExp.lastMatch, match[1]);
        } else {
          treatAsChars = true;
        }
      } else if (html2.charAt(0) === "<") {
        match = this.startTagRe.exec(html2);
        if (match) {
          html2 = RegExp.rightContext;
          treatAsChars = false;
          const tagName = this.parseStartTag(RegExp.lastMatch, match[1], match);
          if (tagName === "script" || tagName === "style") {
            index = html2.search(new RegExp(`</${tagName}`, "i"));
            if (index !== -1) {
              this.scanner.characters(html2.substring(0, index));
              html2 = html2.substring(index);
              treatAsChars = false;
            } else {
              treatAsChars = true;
            }
          }
        } else {
          treatAsChars = true;
        }
      }
      if (treatAsChars) {
        index = html2.indexOf("<");
        let offset = index;
        if (index === 0) {
          index = html2.substring(1).indexOf("<");
          offset = offset + 1;
        }
        if (index === -1) {
          characters = html2;
          html2 = "";
        } else {
          characters = html2.substring(0, offset);
          html2 = html2.substring(offset);
        }
        if (!this.options.ignoreWhitespaceText || !/^\s*$/.test(characters))
          this.scanner.characters(characters);
      }
      treatAsChars = true;
      match = null;
    }
  }
  parseStartTag(input, tagName, match) {
    const isSelfColse = selfCloseTagRe.test(input);
    let attrInput = match[2];
    if (isSelfColse)
      attrInput = attrInput.replace(/\s*\/\s*$/, "");
    const attrs = this.parseAttributes(tagName, attrInput);
    this.scanner.startElement(tagName, attrs, isSelfColse, match[0]);
    return tagName.toLocaleLowerCase();
  }
  parseEndTag(input, tagName) {
    this.scanner.endElement(tagName);
  }
  parseAttributes(tagName, input) {
    const attrs = {};
    input.replace(this.attrRe, (...m) => {
      const [_attr, name, _c2, value, _c4, valueInQuote, _c6, valueInSingleQuote] = m;
      attrs[name] = valueInSingleQuote ?? valueInQuote ?? value ?? true;
      return void 0;
    });
    return attrs;
  }
};

// src/vdomparser.ts
function vdom(obj = null) {
  if (obj instanceof VNode)
    return obj;
  if (obj instanceof Buffer)
    obj = obj.toString("utf-8");
  if (typeof obj === "string")
    return parseHTML(obj);
  return new VDocumentFragment();
}
function parseHTML(html2) {
  if (typeof html2 !== "string") {
    console.error("parseHTML requires string, found", html2);
    throw new Error("parseHTML requires string");
  }
  const frag = html2.indexOf("<!") === 0 ? new VHTMLDocument(true) : new VDocumentFragment();
  const stack = [frag];
  const parser = new HtmlParser({
    // the for methods must be implemented yourself
    scanner: {
      startElement(tagName, attrs, isSelfClosing) {
        const lowerTagName = tagName.toLowerCase();
        if (lowerTagName === "!doctype") {
          frag.docType = new VDocType();
          return;
        }
        for (const name in attrs) {
          if (hasOwn(attrs, name)) {
            const value = attrs[name];
            if (typeof value === "string")
              attrs[name] = unescapeHTML(value);
          }
        }
        const parentNode = stack[stack.length - 1];
        if (parentNode) {
          const element = document.createElement(tagName, attrs);
          parentNode.appendChild(element);
          if (!(SELF_CLOSING_TAGS.includes(tagName.toLowerCase()) || isSelfClosing)) {
            stack.push(element);
          }
        }
      },
      endElement(_tagName) {
        stack.pop();
      },
      characters(text) {
        text = unescapeHTML(text);
        const parentNode = stack[stack.length - 1];
        if (parentNode?.lastChild?.nodeType === VNode.TEXT_NODE) {
          parentNode.lastChild._text += text;
        } else {
          if (parentNode)
            parentNode.appendChild(new VTextNode(text));
        }
      },
      comment(_text) {
      }
    }
  });
  parser.parse(html2);
  return frag;
}
VElement.prototype.setInnerHTML = function(html2) {
  const frag = parseHTML(html2);
  this._childNodes = frag._childNodes;
  this._fixChildNodesParent();
};

// src/manipulate.ts
function handleHTML(html2, handler) {
  const document2 = parseHTML(html2);
  handler(document2);
  return document2.render();
}

// src/serialize-markdown.ts
function serialize(node, context = {
  level: 0,
  count: 0
}) {
  if (node.nodeType === VNode.DOCUMENT_FRAGMENT_NODE) {
    return node.children.map((c) => serialize(c, { ...context })).join("");
  } else if (isVElement(node)) {
    const tag = node.tagName.toLowerCase();
    const handleChildren = (ctx) => node.children.map((c) => serialize(c, { ...context, ...ctx })).join("");
    const rules = {
      b: () => `**${handleChildren()}**`,
      strong: () => `**${handleChildren()}**`,
      i: () => `*${handleChildren()}*`,
      em: () => `*${handleChildren()}*`,
      u: () => `<u>${handleChildren()}</u>`,
      mark: () => `==${handleChildren()}==`,
      tt: () => `==${handleChildren()}==`,
      code: () => `==${handleChildren()}==`,
      strike: () => `~~${handleChildren()}~~`,
      sub: () => `~${handleChildren()}~`,
      super: () => `^${handleChildren()}^`,
      sup: () => `^${handleChildren()}^`,
      li: () => `- ${handleChildren()}
`,
      // todo numbered
      br: () => `${handleChildren()}
`,
      ol: () => `

${handleChildren({ level: context.level + 1 })}

`,
      // todo indent
      ul: () => `

${handleChildren({ level: context.level + 1 })}

`,
      // todo indent
      blockquote: () => `

> ${handleChildren()}

`,
      // todo continue '>'
      pre: () => `

\`\`\`
${handleChildren()}
\`\`\`

`,
      p: () => `

${handleChildren()}

`,
      div: () => `

${handleChildren()}

`,
      h1: () => `

# ${handleChildren()}

`,
      h2: () => `

## ${handleChildren()}

`,
      h3: () => `

### ${handleChildren()}

`,
      h4: () => `

#### ${handleChildren()}

`,
      h5: () => `

##### ${handleChildren()}

`,
      h6: () => `

###### ${handleChildren()}

`,
      hr: () => `

---

`,
      a: () => `[${handleChildren()}](${node.getAttribute("href") ?? "#"})`,
      img: () => `![${node.getAttribute("alt") ?? ""}](${node.getAttribute("src") ?? ""})`
      // todo audio, video and other HTML stuff
    };
    const fn = rules[tag];
    if (fn)
      return fn();
    else
      return handleChildren();
  }
  return node.textContent ?? "";
}
function serializeMarkdown(node) {
  return `${serialize(node).replace(/\n{2,}/g, "\n\n").trim()}
`;
}

// src/tidy.ts
var SELECTOR_BLOCK_ELEMENTS = "meta,link,script,p,h1,h2,h3,h4,h5,h6,blockquote,div,ul,ol,li,article,section,footer,head,body,title,nav,hr,form";
var TAGS_KEEP_CONTENT = ["PRE", "CODE", "SCRIPT", "STYLE", "TT"];
function level(element) {
  let indent = "";
  while (element.parentNode) {
    indent += "  ";
    element = element.parentNode;
  }
  return indent.substr(2);
}
function tidyDOM(document2) {
  document2.handle(SELECTOR_BLOCK_ELEMENTS, (e) => {
    let ee = e;
    while (ee) {
      if (TAGS_KEEP_CONTENT.includes(ee.tagName))
        return;
      ee = ee.parentNode;
    }
    const prev = e.previousSibling;
    if (!prev || prev.nodeType !== VNode.TEXT_NODE || !prev.nodeValue?.endsWith("\n")) {
      e.parentNode?.insertBefore(new VTextNode("\n"), e);
    }
    e.parentNode?.insertBefore(new VTextNode(level(e)), e);
    const next = e.nextSibling;
    if (!next || next.nodeType !== VNode.TEXT_NODE || !next.nodeValue?.startsWith("\n")) {
      if (next)
        e.parentNode?.insertBefore(new VTextNode("\n"), next);
      else
        e.parentNode?.appendChild(new VTextNode("\n"));
    }
    if (e.childNodes.length) {
      const first = e.firstChild;
      if (first.nodeType === VNode.TEXT_NODE)
        e.insertBefore(new VTextNode(`
${level(e)}  `));
      e.appendChild(new VTextNode(`
${level(e)}`));
    }
  });
}

// src/serialize-plaintext.ts
function serialize2(node, context = {
  level: 0,
  count: 0
}) {
  if (node.nodeType === VNode.DOCUMENT_FRAGMENT_NODE) {
    return node.children.map((c) => serialize2(c, { ...context })).join("");
  } else if (isVElement(node)) {
    const tag = node.tagName.toLowerCase();
    const handleChildren = (ctx) => node.children.map((c) => serialize2(c, { ...context, ...ctx })).join("");
    const rules = {
      br: () => `${handleChildren()}
`,
      title: () => "",
      script: () => "",
      style: () => ""
    };
    SELECTOR_BLOCK_ELEMENTS.split(",").forEach((tag2) => {
      rules[tag2] = () => `

${handleChildren().trim()}

`;
    });
    const fn = rules[tag];
    if (fn)
      return fn();
    else
      return handleChildren();
  }
  return node.textContent ?? "";
}
function serializePlaintext(node) {
  return `${serialize2(node).replace(/\n{2,}/g, "\n\n").trim()}
`;
}

// src/serialize-safehtml.ts
var SELECTOR_BLOCK_ELEMENTS2 = "p,h1,h2,h3,h4,h5,h6,blockquote,div,ul,ol,li,article,section,footer,nav,hr,form";
function serialize3(node, context = {
  level: 0,
  count: 0
}) {
  if (node.nodeType === VNode.DOCUMENT_FRAGMENT_NODE) {
    return node.children.map((c) => serialize3(c, { ...context })).join("");
  } else if (isVElement(node)) {
    const tag = node.tagName?.toLowerCase();
    const handleChildren = (ctx) => node.children.map((c) => serialize3(c, { ...context, ...ctx })).join("");
    const rules = {
      a: () => `<a href="${escapeHTML(node.getAttribute("href") ?? "")}" rel="noopener noreferrer" target="_blank">${handleChildren()}</a>`,
      img: () => `<img src="${escapeHTML(node.getAttribute("src") ?? "")}" alt="${escapeHTML(node.getAttribute("alt") ?? "")}">`,
      br: () => `<br>`,
      title: () => "",
      script: () => "",
      style: () => "",
      head: () => ""
    };
    SELECTOR_BLOCK_ELEMENTS2.split(",").forEach((tag2) => {
      rules[tag2] = () => `<${tag2}>${handleChildren().trim()}</${tag2}>`;
    });
    const fn = rules[tag];
    if (fn)
      return fn();
    return handleChildren();
  }
  return escapeHTML(node.textContent ?? "");
}
function serializeSafeHTML(node) {
  return serialize3(node).trim();
}
function safeHTML(html2) {
  return serializeSafeHTML(parseHTML(html2));
}

// src/xml.ts
function xml(itag, iattrs, ...ichildren) {
  const { tag, attrs, children } = hArgumentParser(itag, iattrs, ichildren);
  return markup(true, tag, attrs, children);
}
xml.firstLine = '<?xml version="1.0" encoding="utf-8"?>';
xml.xml = true;

export {
  vdom,
  parseHTML,
  handleHTML,
  serializeMarkdown,
  tidyDOM,
  serializePlaintext,
  serializeSafeHTML,
  safeHTML,
  xml
};
//# sourceMappingURL=chunk-MR7V3FOC.js.map