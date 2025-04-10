import { Node } from "@tiptap/core";
const Alert = Node.create({
  name: "alert",
  content: "inline+",
  marks: "",
  group: "block",
  inline: !1,
  selectable: !0,
  draggable: !0,
  parseHTML() {
    return [
      {
        tag: "p.info",
        priority: 60
      },
      {
        tag: "p.warning",
        priority: 60
      },
      {
        tag: "div.info",
        priority: 60
      },
      {
        tag: "div.warning",
        priority: 60
      }
    ];
  },
  addAttributes() {
    return {
      class: {
        default: "info",
        parseHTML: (element) => element.getAttribute("class")
      }
    };
  },
  renderHTML({ HTMLAttributes }) {
    return ["div", HTMLAttributes, 0];
  }
});
export {
  Alert
};
//# sourceMappingURL=alert.js.map
