'use strict';

var core = require('@tiptap/core');
var model = require('@tiptap/pm/model');
var zeedDom = require('zeed-dom');

/**
 * Returns the HTML string representation of a given document node.
 *
 * @param doc - The document node to serialize.
 * @param schema - The Prosemirror schema to use for serialization.
 * @returns The HTML string representation of the document fragment.
 *
 * @example
 * ```typescript
 * const html = getHTMLFromFragment(doc, schema)
 * ```
 */
function getHTMLFromFragment(doc, schema, options) {
    // Use zeed-dom for serialization.
    const zeedDocument = model.DOMSerializer.fromSchema(schema).serializeFragment(doc.content, {
        document: zeedDom.createHTMLDocument(),
    });
    return zeedDocument.render();
}

/**
 * Generates HTML from a ProseMirror JSON content object.
 * @param doc - The ProseMirror JSON content object.
 * @param extensions - The Tiptap extensions used to build the schema.
 * @returns The generated HTML string.
 * @example
 * const doc = {
 *   type: 'doc',
 *   content: [
 *     {
 *       type: 'paragraph',
 *       content: [
 *         {
 *           type: 'text',
 *           text: 'Hello world!'
 *         }
 *       ]
 *     }
 *   ]
 * }
 * const extensions = [...]
 * const html = generateHTML(doc, extensions)
 */
function generateHTML(doc, extensions) {
    const schema = core.getSchema(extensions);
    const contentNode = model.Node.fromJSON(schema, doc);
    return getHTMLFromFragment(contentNode, schema);
}

/**
 * Generates a JSON object from the given HTML string and converts it into a Prosemirror node with content.
 * @param {string} html - The HTML string to be converted into a Prosemirror node.
 * @param {Extensions} extensions - The extensions to be used for generating the schema.
 * @param {ParseOptions} options - The options to be supplied to the parser.
 * @returns {Record<string, any>} - The generated JSON object.
 * @example
 * const html = '<p>Hello, world!</p>'
 * const extensions = [...]
 * const json = generateJSON(html, extensions)
 * console.log(json) // { type: 'doc', content: [{ type: 'paragraph', content: [{ type: 'text', text: 'Hello, world!' }] }] }
 */
function generateJSON(html, extensions, options) {
    const schema = core.getSchema(extensions);
    const dom = zeedDom.parseHTML(html);
    return model.DOMParser.fromSchema(schema).parse(dom, options).toJSON();
}

exports.generateHTML = generateHTML;
exports.generateJSON = generateJSON;
//# sourceMappingURL=index.cjs.map
