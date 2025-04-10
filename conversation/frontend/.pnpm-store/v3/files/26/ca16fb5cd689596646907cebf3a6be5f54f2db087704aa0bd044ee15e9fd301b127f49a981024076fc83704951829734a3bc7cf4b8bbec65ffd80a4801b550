import { mergeAttributes } from "@tiptap/core";
import { Heading } from "@tiptap/extension-heading";
import "@tiptap/extension-text-style";
const CustomHeading = Heading.extend({
  name: "customHeading",
  addOptions() {
    var _a;
    return {
      ...(_a = this.parent) == null ? void 0 : _a.call(this),
      HTMLAttributes: {}
    };
  },
  parseHTML() {
    return this.options.levels.map((level) => ({
      tag: `h${level}`,
      attrs: { level }
    }));
  },
  renderHTML({ node, HTMLAttributes }) {
    return [
      `h${this.options.levels.includes(node.attrs.level) ? node.attrs.level : this.options.levels[0]}`,
      mergeAttributes(this.options.HTMLAttributes, HTMLAttributes),
      0
    ];
  },
  addCommands() {
    return {
      setCustomHeading: (attributes) => ({ tr, dispatch, commands }) => {
        if (!this.options.levels.includes(attributes.level))
          return !1;
        const { selection } = tr, { from, to } = selection;
        return tr.doc.nodesBetween(from, to, (node, pos) => {
          node.isBlock && from >= pos && to <= pos + node.nodeSize && node.content.forEach((content) => {
            content.marks.forEach((mark) => {
              mark.type.name === "textStyle" && mark.attrs.fontSize && mark.attrs.fontSize !== null && (tr = tr.removeMark(pos, pos + node.nodeSize, mark.type));
            });
          });
        }), dispatch && dispatch(tr), commands.setHeading({ level: attributes.level });
      }
    };
  }
});
export {
  CustomHeading
};
//# sourceMappingURL=heading.js.map
