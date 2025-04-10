import {
  handleHTML,
  parseHTML,
  safeHTML,
  serializeMarkdown,
  serializePlaintext,
  serializeSafeHTML,
  tidyDOM,
  vdom,
  xml
} from "./chunk-MR7V3FOC.js";
import {
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
  hasOwn,
  html,
  isVDocument,
  isVElement,
  isVTextElement,
  removeBodyContainer,
  unescapeHTML
} from "./chunk-YMRUYF4I.js";

// src/node.ts
import { readFileSync, writeFileSync } from "node:fs";
function handleHTMLFile(filePath, handler, outPath) {
  const html2 = readFileSync(filePath, "utf-8");
  const document2 = parseHTML(html2);
  const htmlIn = document2.render();
  handler(document2);
  const htmlOut = document2.render();
  if (outPath || htmlOut !== htmlIn) {
    writeFileSync(outPath ?? filePath, htmlOut, "utf-8");
    return htmlOut;
  }
  return html2;
}
export {
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
  handleHTMLFile,
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
};
//# sourceMappingURL=index.node.js.map