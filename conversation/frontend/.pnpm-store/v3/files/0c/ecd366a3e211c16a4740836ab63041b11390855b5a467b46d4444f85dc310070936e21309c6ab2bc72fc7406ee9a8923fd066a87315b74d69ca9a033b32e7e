import { Paragraph as Paragraph$1 } from "@tiptap/extension-paragraph";
const Paragraph = Paragraph$1.extend({
  parseHTML() {
    var _a;
    return [
      ...((_a = this.parent) == null ? void 0 : _a.call(this)) ?? [],
      {
        tag: "div[style]:has(> span)"
      }
    ];
  }
});
export {
  Paragraph
};
//# sourceMappingURL=paragraph.js.map
