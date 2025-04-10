import "@tiptap/extension-text-style";
import { Extension } from "@tiptap/core";
const FontSize = Extension.create({
  name: "fontSize",
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
          fontSize: {
            default: null,
            parseHTML: (element) => {
              var _a;
              return (_a = element.style.fontSize) == null ? void 0 : _a.replace(/['"]+/g, "");
            },
            renderHTML: (attributes) => attributes.fontSize ? {
              style: `font-size: ${attributes.fontSize}`
            } : {}
          }
        }
      }
    ];
  },
  addCommands() {
    return {
      setFontSize: (fontSize) => ({ chain }) => chain().setMark("textStyle", { fontSize }).run(),
      unsetFontSize: () => ({ chain }) => chain().setMark("textStyle", { fontSize: null }).removeEmptyTextStyle().run()
    };
  }
});
export {
  FontSize
};
//# sourceMappingURL=font-size.js.map
