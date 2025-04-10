import { Mark, mergeAttributes } from "@tiptap/core";
const Abbr = Mark.create({
  name: "abbr",
  addOptions() {
    return {
      HTMLAttributes: {}
    };
  },
  parseHTML() {
    return [
      {
        tag: "abbr"
      }
    ];
  },
  renderHTML({ HTMLAttributes }) {
    return [
      "abbr",
      mergeAttributes(this.options.HTMLAttributes, HTMLAttributes),
      0
    ];
  },
  addCommands() {
    return {
      setAbbr: () => ({ commands }) => commands.setMark(this.name),
      toggleAbbr: () => ({ commands }) => commands.toggleMark(this.name)
    };
  }
});
export {
  Abbr
};
//# sourceMappingURL=abbr.js.map
