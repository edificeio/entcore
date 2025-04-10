import "@tiptap/extension-text-style";
import { Extension } from "@tiptap/core";
const LineHeight = Extension.create({
  name: "lineHeight",
  addOptions() {
    return {
      types: ["textStyle"]
    };
  },
  addGlobalAttributes() {
    return [
      {
        types: this.options.types,
        attributes: {
          lineHeight: {
            default: null,
            parseHTML: (element) => {
              var _a;
              return (_a = element.style.lineHeight) == null ? void 0 : _a.replace(/['"]+/g, "");
            },
            renderHTML: (attributes) => attributes.lineHeight ? {
              style: `line-height: ${attributes.lineHeight}`
            } : {}
          }
        }
      }
    ];
  }
});
export {
  LineHeight
};
//# sourceMappingURL=line-height.js.map
