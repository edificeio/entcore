import { getSchema } from '@tiptap/core';
import { DOMSerializer, Node, DOMParser } from '@tiptap/pm/model';
import { createHTMLDocument, parseHTML } from 'zeed-dom';

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
    const zeedDocument = DOMSerializer.fromSchema(schema).serializeFragment(doc.content, {
        document: createHTMLDocument(),
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
    const schema = getSchema(extensions);
    const contentNode = Node.fromJSON(schema, doc);
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
    const schema = getSchema(extensions);
    const dom = parseHTML(html);
    return DOMParser.fromSchema(schema).parse(dom, options).toJSON();
}

export { generateHTML, generateJSON };
//# sourceMappingURL=index.js.map
