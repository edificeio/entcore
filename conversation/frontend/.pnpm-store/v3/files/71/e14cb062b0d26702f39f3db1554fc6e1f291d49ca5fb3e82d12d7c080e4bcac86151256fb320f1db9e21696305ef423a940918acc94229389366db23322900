import Highlight from "@tiptap/extension-highlight";
const CustomHighlight = Highlight.extend({
  name: "customHighlight",
  addOptions() {
    var _a;
    return {
      ...(_a = this.parent) == null ? void 0 : _a.call(this),
      multicolor: !0,
      HTMLAttributes: {}
    };
  },
  parseHTML() {
    var _a;
    return [
      {
        ...(_a = this.parent) == null ? void 0 : _a.call(this),
        style: "background-color",
        getAttrs: (style) => ({
          color: style
        })
      }
    ];
  }
});
export {
  CustomHighlight
};
//# sourceMappingURL=highlight.js.map
