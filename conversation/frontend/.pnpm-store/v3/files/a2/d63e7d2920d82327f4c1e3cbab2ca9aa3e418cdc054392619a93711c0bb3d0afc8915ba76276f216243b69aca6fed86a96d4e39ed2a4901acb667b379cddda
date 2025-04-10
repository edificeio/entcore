// src/encoding.ts
import { decodeHTML as decode } from "entities";
function escapeHTML(text) {
  return text.replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;").replace(/'/g, "&apos;").replace(/"/g, "&quot;").replace(/\xA0/g, "&nbsp;").replace(/\xAD/g, "&shy;");
}
var unescapeHTML = (html2) => decode(html2);

// src/h.ts
function _h(context, tag, attrs, children) {
  if (typeof tag === "function") {
    return tag({
      props: { ...attrs, children },
      attrs,
      children,
      h: context.h,
      context
    });
  } else {
    let isElement = true;
    let el;
    if (tag) {
      if (tag.toLowerCase() === "fragment") {
        el = context.document.createDocumentFragment();
        isElement = false;
      } else {
        el = context.document.createElement(tag);
      }
    } else {
      el = context.document.createElement("div");
    }
    if (attrs && isElement) {
      const element = el;
      for (let [key, value] of Object.entries(attrs)) {
        key = key.toString();
        const compareKey = key.toLowerCase();
        if (compareKey === "classname") {
          element.className = value;
        } else if (compareKey === "on") {
          Object.entries(value).forEach(([name, value2]) => {
            element.setAttribute(`on${name}`, String(value2));
          });
        } else if (value !== false && value != null) {
          if (value === true)
            element.setAttribute(key, key);
          else
            element.setAttribute(key, value.toString());
        }
      }
    }
    if (children) {
      for (const childOuter of children) {
        const cc = Array.isArray(childOuter) ? [...childOuter] : [childOuter];
        for (const child of cc) {
          if (child) {
            if (child !== false && child != null) {
              if (typeof child !== "object") {
                el.appendChild(
                  context.document.createTextNode(child.toString())
                );
              } else {
                el.appendChild(child);
              }
            }
          }
        }
      }
    }
    return el;
  }
}
function hArgumentParser(tag, attrs, ...children) {
  if (typeof tag === "object") {
    tag = "fragment";
    children = tag.children;
    attrs = tag.attrs;
  }
  if (Array.isArray(attrs)) {
    children = [attrs];
    attrs = {};
  } else if (attrs) {
    if (attrs.attrs) {
      attrs = { ...attrs.attrs, ...attrs };
      delete attrs.attrs;
    }
  } else {
    attrs = {};
  }
  return {
    tag,
    attrs,
    children: typeof children[0] === "string" ? children : children.flat(Number.POSITIVE_INFINITY)
  };
}
function hFactory(context) {
  context.h = function h3(itag, iattrs, ...ichildren) {
    const { tag, attrs, children } = hArgumentParser(itag, iattrs, ichildren);
    return _h(context, tag, attrs, children);
  };
  return context.h;
}

// src/vcss.ts
import { parse } from "css-what";
function log(..._args) {
}
var cache = {};
function parseSelector(selector) {
  let ast = cache[selector];
  if (ast == null) {
    ast = parse(selector);
    cache[selector] = ast;
  }
  return ast;
}
function matchSelector(selector, element, { debug = false } = {}) {
  for (const rules of parseSelector(selector)) {
    if (debug) {
      log("Selector:", selector);
      log("Rules:", rules);
      log("Element:", element);
    }
    const handleRules = (element2, rules2) => {
      let success = false;
      for (const part of rules2) {
        const { type, name, action, value, _ignoreCase = true, data } = part;
        if (type === "attribute") {
          if (action === "equals") {
            success = element2.getAttribute(name) === value;
            if (debug)
              log("Attribute equals", success);
          } else if (action === "start") {
            success = !!element2.getAttribute(name)?.startsWith(value);
            if (debug)
              log("Attribute start", success);
          } else if (action === "end") {
            success = !!element2.getAttribute(name)?.endsWith(value);
            if (debug)
              log("Attribute start", success);
          } else if (action === "element") {
            if (name === "class") {
              success = element2.classList.contains(value);
              if (debug)
                log("Attribute class", success);
            } else {
              success = !!element2.getAttribute(name)?.includes(value);
              if (debug)
                log("Attribute element", success);
            }
          } else if (action === "exists") {
            success = element2.hasAttribute(name);
            if (debug)
              log("Attribute exists", success);
          } else if (action === "any") {
            success = !!element2.getAttribute(name)?.includes(value);
            if (debug)
              log("Attribute any", success);
          } else {
            console.warn("Unknown CSS selector action", action);
          }
        } else if (type === "tag") {
          success = element2.tagName === name.toUpperCase();
          if (debug)
            log("Is tag", success);
        } else if (type === "universal") {
          success = true;
          if (debug)
            log("Is universal", success);
        } else if (type === "pseudo") {
          if (name === "not") {
            let ok = true;
            data.forEach((rules3) => {
              if (!handleRules(element2, rules3))
                ok = false;
            });
            success = !ok;
          }
          if (debug)
            log("Is :not", success);
        } else {
          console.warn("Unknown CSS selector type", type, selector, rules2);
        }
        if (!success)
          break;
      }
      return success;
    };
    if (handleRules(element, rules))
      return true;
  }
  return false;
}

// src/vdom.ts
var inspect = Symbol.for("nodejs.util.inspect.custom");
var B = { fontWeight: "bold" };
var I = { fontStyle: "italic" };
var M = { backgroundColor: "rgb(255, 250, 165)" };
var U = { textDecorations: "underline" };
var S = { textDecorations: "line-through" };
var DEFAULTS = {
  b: B,
  strong: B,
  em: I,
  i: I,
  mark: M,
  u: U,
  a: U,
  s: S,
  del: S,
  ins: M,
  strike: S
  // 'code': C,
  // 'tt': C
};
function toCamelCase(s) {
  return s.toLowerCase().replace(/[^a-z0-9]+(.)/gi, (_m, chr) => chr.toUpperCase());
}
var _VNode = class _VNode {
  constructor() {
    this.append = this.appendChild;
    this._parentNode = null;
    this._childNodes = [];
  }
  get nodeType() {
    console.error("Subclasses should define nodeType!");
    return 0;
  }
  get nodeName() {
    console.error("Subclasses should define nodeName!");
    return "";
  }
  get nodeValue() {
    return null;
  }
  cloneNode(deep = false) {
    const node = new this.constructor();
    if (deep) {
      node._childNodes = this._childNodes.map((c) => c.cloneNode(true));
      node._fixChildNodesParent();
    }
    return node;
  }
  _fixChildNodesParent() {
    this._childNodes.forEach((node) => node._parentNode = this);
  }
  insertBefore(newNode, node) {
    if (newNode !== node) {
      let index = node ? this._childNodes.indexOf(node) : 0;
      if (index < 0)
        index = 0;
      this._childNodes.splice(index, 0, newNode);
      this._fixChildNodesParent();
    }
  }
  appendChild(node) {
    if (node == null)
      return;
    if (node === this) {
      console.warn("Cannot appendChild to self");
      return;
    }
    if (node instanceof VDocument)
      console.warn("No defined how to append a document to a node!", node);
    if (node instanceof VDocumentFragment) {
      for (const c of [...node._childNodes]) {
        this.appendChild(c);
      }
    } else if (Array.isArray(node)) {
      for (const c of [...node]) {
        this.appendChild(c);
      }
    } else if (node instanceof _VNode) {
      node.remove();
      this._childNodes.push(node);
    } else {
      try {
        const text = typeof node === "string" ? node : JSON.stringify(node, null, 2);
        this._childNodes.push(new VTextNode(text));
      } catch (err) {
        console.error(`The data ${node} to be added to ${this.render()} is problematic: ${err}`);
      }
    }
    this._fixChildNodesParent();
  }
  removeChild(node) {
    const i = this._childNodes.indexOf(node);
    if (i >= 0) {
      node._parentNode = null;
      this._childNodes.splice(i, 1);
      this._fixChildNodesParent();
    }
  }
  /** Remove node */
  remove() {
    this?.parentNode?.removeChild(this);
    return this;
  }
  /** Replace content of node with text or nodes */
  replaceChildren(...nodes) {
    this._childNodes = nodes.map(
      (n) => typeof n === "string" ? new VTextNode(n) : n.remove()
    );
    this._fixChildNodesParent();
  }
  /** Replace node itself with nodes */
  replaceWith(...nodes) {
    const p = this._parentNode;
    if (p) {
      const index = this._indexInParent();
      if (index >= 0) {
        nodes = nodes.map(
          (n) => typeof n === "string" ? new VTextNode(n) : n.remove()
        );
        p._childNodes.splice(index, 1, ...nodes);
        this._parentNode = null;
        p._fixChildNodesParent();
      }
    }
  }
  _indexInParent() {
    if (this._parentNode)
      return this._parentNode.childNodes.indexOf(this);
    return -1;
  }
  get parentNode() {
    return this._parentNode;
  }
  get childNodes() {
    return this._childNodes || [];
  }
  get children() {
    return this._childNodes || [];
  }
  get firstChild() {
    return this._childNodes[0];
  }
  get lastChild() {
    return this._childNodes[this._childNodes.length - 1];
  }
  get nextSibling() {
    const i = this._indexInParent();
    if (i != null)
      return this.parentNode.childNodes[i + 1] || null;
    return null;
  }
  get previousSibling() {
    const i = this._indexInParent();
    if (i > 0)
      return this.parentNode.childNodes[i - 1] || null;
    return null;
  }
  flatten() {
    const elements = [];
    if (this instanceof VElement)
      elements.push(this);
    for (const child of this._childNodes)
      elements.push(...child.flatten());
    return elements;
  }
  flattenNodes() {
    const nodes = [];
    nodes.push(this);
    for (const child of this._childNodes)
      nodes.push(...child.flattenNodes());
    return nodes;
  }
  render() {
    return "";
  }
  get textContent() {
    return this._childNodes.map((c) => c.textContent).join("");
  }
  set textContent(text) {
    this._childNodes = [];
    if (text)
      this.appendChild(new VTextNode(text.toString()));
  }
  contains(otherNode) {
    if (otherNode === this)
      return true;
    return this._childNodes.some((n) => n.contains(otherNode));
  }
  get ownerDocument() {
    if (this.nodeType === _VNode.DOCUMENT_NODE || this.nodeType === _VNode.DOCUMENT_FRAGMENT_NODE)
      return this;
    return this?._parentNode?.ownerDocument;
  }
  toString() {
    return `${this.nodeName}`;
  }
  [inspect]() {
    return `${this.constructor.name} "${this.render()}"`;
  }
};
_VNode.ELEMENT_NODE = 1;
_VNode.TEXT_NODE = 3;
_VNode.CDATA_SECTION_NODE = 4;
_VNode.PROCESSING_INSTRUCTION_NODE = 7;
_VNode.COMMENT_NODE = 8;
_VNode.DOCUMENT_NODE = 9;
_VNode.DOCUMENT_TYPE_NODE = 10;
_VNode.DOCUMENT_FRAGMENT_NODE = 11;
var VNode = _VNode;
var VTextNode = class extends VNode {
  get nodeType() {
    return VNode.TEXT_NODE;
  }
  get nodeName() {
    return "#text";
  }
  get nodeValue() {
    return this._text || "";
  }
  get textContent() {
    return this.nodeValue;
  }
  constructor(text = "") {
    super();
    this._text = text;
  }
  render() {
    const parentTagName = this.parentNode?.tagName;
    if (parentTagName === "SCRIPT" || parentTagName === "STYLE")
      return this._text;
    return escapeHTML(this._text);
  }
  cloneNode(deep = false) {
    const node = super.cloneNode(deep);
    node._text = this._text;
    return node;
  }
};
var VNodeQuery = class extends VNode {
  getElementById(name) {
    return this.flatten().find((e) => e._attributes.id === name);
  }
  getElementsByClassName(name) {
    return this.flatten().filter((e) => e.classList.contains(name));
  }
  matches(selector) {
    return matchSelector(selector, this);
  }
  querySelectorAll(selector) {
    return this.flatten().filter((e) => e.matches(selector));
  }
  querySelector(selector) {
    return this.flatten().find((e) => e.matches(selector));
  }
  //
  parent(selector) {
    if (this.matches(selector))
      return this;
    if (this.parentNode == null)
      return null;
    return this.parentNode?.parent(selector);
  }
  handle(selector, handler) {
    let i = 0;
    for (const el of this.querySelectorAll(selector))
      handler(el, i++);
  }
};
var VElement = class extends VNodeQuery {
  get nodeType() {
    return VNode.ELEMENT_NODE;
  }
  get nodeName() {
    return this._nodeName;
  }
  constructor(name = "div", attrs = {}) {
    super();
    this._originalTagName = name;
    this._nodeName = (name || "").toUpperCase();
    this._attributes = attrs || {};
  }
  cloneNode(deep = false) {
    const node = super.cloneNode(deep);
    node._originalTagName = this._originalTagName;
    node._nodeName = this._nodeName;
    node._attributes = Object.assign({}, this._attributes);
    return node;
  }
  get attributes() {
    return Object.entries(this._attributes).map(([name, value]) => ({ name, value }));
  }
  get attributesObject() {
    return { ...this._attributes };
  }
  _findAttributeName(name) {
    const search = name.toLowerCase();
    return Object.keys(this._attributes).find(
      (name2) => search === name2.toLowerCase()
    ) || null;
  }
  setAttribute(name, value) {
    this.removeAttribute(name);
    this._attributes[name] = value;
    this._styles = void 0;
    this._dataset = void 0;
  }
  getAttribute(name) {
    const originalName = this._findAttributeName(name);
    const value = originalName ? this._attributes[originalName] : null;
    if (value == null)
      return null;
    else if (typeof value === "string")
      return value;
    else
      return "";
  }
  removeAttribute(name) {
    const originalName = this._findAttributeName(String(name));
    if (originalName)
      delete this._attributes[name];
  }
  hasAttribute(name) {
    const originalName = this._findAttributeName(name);
    return originalName ? this._attributes[originalName] != null : false;
  }
  /// See https://developer.mozilla.org/en-US/docs/Web/API/HTMLElement/style
  get style() {
    if (this._styles == null) {
      const styles = {};
      let count = 0;
      const styleString = this.getAttribute("style");
      if (styleString) {
        let m;
        const re = /\s*([\w-]+)\s*:\s*((url\(.*?\)[^;]*|[^;]+))/gi;
        while (m = re.exec(styleString)) {
          ++count;
          const name = m[1];
          const value = m[2].trim();
          styles[name] = value;
          styles[toCamelCase(name)] = value;
        }
      }
      this._styles = {
        get length() {
          return count;
        },
        getPropertyValue(name) {
          return styles[name];
        },
        ...DEFAULTS[this.tagName.toLowerCase()],
        ...styles
      };
    }
    return this._styles;
  }
  /// See https://developer.mozilla.org/en-US/docs/Web/API/HTMLElement/dataset
  get dataset() {
    if (this._dataset == null) {
      const dataset = {};
      for (const [key, value] of Object.entries(this._attributes)) {
        if (key.startsWith("data-")) {
          dataset[key.slice(5)] = value;
          dataset[toCamelCase(key.slice(5))] = value;
        }
      }
      this._dataset = dataset;
    }
    return this._dataset;
  }
  get tagName() {
    return this._nodeName;
  }
  /** Private function to easily change the tagName */
  setTagName(name) {
    this._nodeName = name.toUpperCase();
  }
  get id() {
    return this._attributes.id || null;
  }
  set id(value) {
    if (value == null)
      delete this._attributes.id;
    else this._attributes.id = value;
  }
  get src() {
    return this._attributes.src;
  }
  set src(value) {
    if (value == null)
      delete this._attributes.src;
    else this._attributes.src = value;
  }
  //
  getElementsByTagName(name) {
    name = name.toUpperCase();
    const elements = this.flatten();
    if (name !== "*")
      return elements.filter((e) => e.tagName === name);
    return elements;
  }
  // html
  setInnerHTML(_html) {
  }
  get innerHTML() {
    return this._childNodes.map((c) => c.render(html)).join("");
  }
  set innerHTML(html2) {
    this.setInnerHTML(html2);
  }
  get outerHTML() {
    return this.render(htmlVDOM);
  }
  // class
  get className() {
    return this._attributes.class || "";
  }
  set className(name) {
    if (Array.isArray(name)) {
      name = name.filter((n) => !!n).join(" ");
    } else if (typeof name === "object") {
      name = Object.entries(name).filter(([_k, v]) => !!v).map(([k, _v]) => k).join(" ");
    }
    this._attributes.class = name;
  }
  get classList() {
    const classNames = String(this.className ?? "").trim().split(/\s+/g) || [];
    return {
      contains(s) {
        return classNames.includes(s);
      },
      add: (s) => {
        if (!classNames.includes(s)) {
          classNames.push(s);
          this.className = classNames;
        }
      },
      remove: (s) => {
        const index = classNames.indexOf(s);
        if (index >= 0) {
          classNames.splice(index, 1);
          this.className = classNames;
        }
      }
    };
  }
  //
  render(h3 = htmlVDOM) {
    return h3(
      this._originalTagName || this.tagName,
      this._attributes,
      this._childNodes.map((c) => c.render(h3)).join("")
      // children:string is not escaped again
    );
  }
};
var VDocType = class _VDocType extends VNode {
  get nodeName() {
    return super.nodeName;
  }
  get nodeValue() {
    return super.nodeValue;
  }
  get nodeType() {
    return _VDocType.DOCUMENT_TYPE_NODE;
  }
  render() {
    return "<!DOCTYPE html>";
  }
};
var VDocumentFragment = class _VDocumentFragment extends VNodeQuery {
  get nodeType() {
    return VNode.DOCUMENT_FRAGMENT_NODE;
  }
  get nodeName() {
    return "#document-fragment";
  }
  render(h3 = htmlVDOM) {
    return this._childNodes.map((c) => c.render(h3) || []).join("");
  }
  get innerHTML() {
    return this._childNodes.map((c) => c.render(html)).join("");
  }
  createElement(name, attrs = {}) {
    return new VElement(name, attrs);
  }
  createDocumentFragment() {
    return new _VDocumentFragment();
  }
  createTextNode(text) {
    return new VTextNode(text);
  }
};
var VDocument = class extends VDocumentFragment {
  get nodeType() {
    return VNode.DOCUMENT_NODE;
  }
  get nodeName() {
    return "#document";
  }
  get documentElement() {
    return this.firstChild;
  }
  render(h3 = htmlVDOM) {
    let content = super.render(h3);
    if (this.docType)
      content = this.docType.render() + content;
    return content;
  }
};
var VHTMLDocument = class extends VDocument {
  constructor(empty = false) {
    super();
    this.docType = new VDocType();
    if (!empty) {
      const html2 = new VElement("html");
      const body = new VElement("body");
      const head = new VElement("head");
      const title = new VElement("title");
      html2.appendChild(head);
      head.appendChild(title);
      html2.appendChild(body);
      this.appendChild(html2);
    }
  }
  get body() {
    let body = this.querySelector("body");
    if (!body) {
      let html2 = this.querySelector("html");
      if (!html2) {
        html2 = new VElement("html");
        this.appendChild(html2);
      }
      body = new VElement("body");
      html2.appendChild(html2);
    }
    return body;
  }
  get title() {
    return this.querySelector("title")?.textContent || "";
  }
  set title(title) {
    const titleElement = this.querySelector("title");
    if (titleElement)
      titleElement.textContent = title;
  }
  get head() {
    let head = this.querySelector("head");
    if (!head) {
      let html2 = this.querySelector("html");
      if (!html2) {
        html2 = new VElement("html");
        this.appendChild(html2);
      }
      head = new VElement("head");
      html2.insertBefore(html2);
    }
    return head;
  }
};
function createDocument() {
  return new VDocument();
}
function createHTMLDocument() {
  return new VHTMLDocument();
}
var document = createDocument();
var h = hFactory({ document });
function isVElement(n) {
  return n.nodeType === VNode.ELEMENT_NODE;
}
function isVTextElement(n) {
  return n.nodeType === VNode.TEXT_NODE;
}
function isVDocument(n) {
  return n.nodeType === VNode.DOCUMENT_NODE;
}

// src/utils.ts
function removeBodyContainer(body) {
  const ehead = body.querySelector("head");
  const ebody = body.querySelector("body");
  if (ebody || ehead) {
    const body2 = new VDocumentFragment();
    if (ehead) {
      body2.appendChild(ehead.childNodes);
    }
    if (ebody) {
      body2.appendChild(ebody.children);
    }
    return body2;
  }
  return body;
}
var object = {};
var hasOwnProperty = object.hasOwnProperty;
function hasOwn(object2, propertyName) {
  return hasOwnProperty.call(object2, propertyName);
}

// src/html.ts
var SELF_CLOSING_TAGS = [
  "area",
  "base",
  "br",
  "col",
  "embed",
  "hr",
  "img",
  "input",
  "keygen",
  "link",
  "meta",
  "param",
  "source",
  "track",
  "wbr",
  "command"
];
var CDATA = (s) => `<![CDATA[${s}]]>`;
function markup(xmlMode, tag, attrs = {}, children) {
  const hasChildren = !(typeof children === "string" && children === "" || Array.isArray(children) && (children.length === 0 || children.length === 1 && children[0] === "") || children == null);
  const parts = [];
  tag = tag.replace(/__/g, ":");
  if (tag !== "noop" && tag !== "") {
    if (tag !== "cdata")
      parts.push(`<${tag}`);
    else
      parts.push("<![CDATA[");
    for (let name in attrs) {
      if (name && hasOwn(attrs, name)) {
        const v = attrs[name];
        if (name === "html")
          continue;
        if (name.toLowerCase() === "classname")
          name = "class";
        name = name.replace(/__/g, ":");
        if (v === true) {
          parts.push(` ${name}`);
        } else if (name === "style" && typeof v === "object") {
          parts.push(
            ` ${name}="${Object.keys(v).filter((k) => v[k] != null).map((k) => {
              let vv = v[k];
              vv = typeof vv === "number" ? `${vv}px` : vv;
              return `${k.replace(/([a-z])([A-Z])/g, "$1-$2").toLowerCase()}:${vv}`;
            }).join(";")}"`
          );
        } else if (v !== false && v != null) {
          parts.push(` ${name}="${escapeHTML(v.toString())}"`);
        }
      }
    }
    if (tag !== "cdata") {
      if (xmlMode && !hasChildren) {
        parts.push(" />");
        return parts.join("");
      } else {
        parts.push(">");
      }
    }
    if (!xmlMode && SELF_CLOSING_TAGS.includes(tag))
      return parts.join("");
  }
  if (hasChildren) {
    if (typeof children === "string") {
      parts.push(children);
    } else if (children && children.length > 0) {
      for (let child of children) {
        if (child != null && child !== false) {
          if (!Array.isArray(child))
            child = [child];
          for (const c of child) {
            if (c.startsWith("<") && c.endsWith(">") || tag === "script" || tag === "style") {
              parts.push(c);
            } else {
              parts.push(escapeHTML(c.toString()));
            }
          }
        }
      }
    }
  }
  if (attrs.html)
    parts.push(attrs.html);
  if (tag !== "noop" && tag !== "") {
    if (tag !== "cdata")
      parts.push(`</${tag}>`);
    else
      parts.push("]]>");
  }
  return parts.join("");
}
function html(itag, iattrs, ...ichildren) {
  const { tag, attrs, children } = hArgumentParser(itag, iattrs, ichildren);
  return markup(false, tag, attrs, children);
}
var htmlVDOM = markup.bind(null, false);
html.firstLine = "<!DOCTYPE html>";
html.html = true;
var h2 = html;

export {
  escapeHTML,
  unescapeHTML,
  hArgumentParser,
  hFactory,
  VNode,
  VTextNode,
  VNodeQuery,
  VElement,
  VDocType,
  VDocumentFragment,
  VDocument,
  VHTMLDocument,
  createDocument,
  createHTMLDocument,
  document,
  h,
  isVElement,
  isVTextElement,
  isVDocument,
  removeBodyContainer,
  hasOwn,
  SELF_CLOSING_TAGS,
  CDATA,
  markup,
  html,
  h2
};
//# sourceMappingURL=chunk-YMRUYF4I.js.map