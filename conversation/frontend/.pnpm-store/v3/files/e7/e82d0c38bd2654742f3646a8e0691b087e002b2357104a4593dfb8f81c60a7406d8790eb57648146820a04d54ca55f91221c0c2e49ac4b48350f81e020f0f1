"use strict";
var __defProp = Object.defineProperty;
var __getOwnPropDesc = Object.getOwnPropertyDescriptor;
var __getOwnPropNames = Object.getOwnPropertyNames;
var __hasOwnProp = Object.prototype.hasOwnProperty;
var __export = (target, all) => {
  for (var name in all)
    __defProp(target, name, { get: all[name], enumerable: true });
};
var __copyProps = (to, from, except, desc) => {
  if (from && typeof from === "object" || typeof from === "function") {
    for (let key of __getOwnPropNames(from))
      if (!__hasOwnProp.call(to, key) && key !== except)
        __defProp(to, key, { get: () => from[key], enumerable: !(desc = __getOwnPropDesc(from, key)) || desc.enumerable });
  }
  return to;
};
var __toCommonJS = (mod) => __copyProps(__defProp({}, "__esModule", { value: true }), mod);

// src/index.browser.ts
var index_browser_exports = {};
__export(index_browser_exports, {
  CDATA: () => CDATA,
  VDocType: () => VDocType,
  VDocument: () => VDocument,
  VDocumentFragment: () => VDocumentFragment,
  VElement: () => VElement,
  VHTMLDocument: () => VHTMLDocument,
  VNode: () => VNode,
  VNodeQuery: () => VNodeQuery,
  VTextNode: () => VTextNode,
  createDocument: () => createDocument,
  createHTMLDocument: () => createHTMLDocument,
  document: () => document,
  escapeHTML: () => escapeHTML,
  h: () => h,
  hArgumentParser: () => hArgumentParser,
  hFactory: () => hFactory,
  handleHTML: () => handleHTML,
  hasOwn: () => hasOwn,
  html: () => html,
  isVDocument: () => isVDocument,
  isVElement: () => isVElement,
  isVTextElement: () => isVTextElement,
  parseHTML: () => parseHTML,
  removeBodyContainer: () => removeBodyContainer,
  safeHTML: () => safeHTML,
  serializeMarkdown: () => serializeMarkdown,
  serializePlaintext: () => serializePlaintext,
  serializeSafeHTML: () => serializeSafeHTML,
  tidyDOM: () => tidyDOM,
  unescapeHTML: () => unescapeHTML,
  vdom: () => vdom,
  xml: () => xml
});
module.exports = __toCommonJS(index_browser_exports);

// src/encoding.ts
var import_entities = require("entities");
function escapeHTML(text) {
  return text.replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;").replace(/'/g, "&apos;").replace(/"/g, "&quot;").replace(/\xA0/g, "&nbsp;").replace(/\xAD/g, "&shy;");
}
var unescapeHTML = (html2) => (0, import_entities.decodeHTML)(html2);

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
var import_css_what = require("css-what");
function log(..._args) {
}
var cache = {};
function parseSelector(selector) {
  let ast = cache[selector];
  if (ast == null) {
    ast = (0, import_css_what.parse)(selector);
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
// Annotate the CommonJS export names for ESM import in node:
0 && (module.exports = {
  CDATA,
  VDocType,
  VDocument,
  VDocumentFragment,
  VElement,
  VHTMLDocument,
  VNode,
  VNodeQuery,
  VTextNode,
  createDocument,
  createHTMLDocument,
  document,
  escapeHTML,
  h,
  hArgumentParser,
  hFactory,
  handleHTML,
  hasOwn,
  html,
  isVDocument,
  isVElement,
  isVTextElement,
  parseHTML,
  removeBodyContainer,
  safeHTML,
  serializeMarkdown,
  serializePlaintext,
  serializeSafeHTML,
  tidyDOM,
  unescapeHTML,
  vdom,
  xml
});
//# sourceMappingURL=index.browser.cjs.map