import { Node, mergeAttributes } from "@tiptap/core";
const Linker = Node.create({
  name: "linker",
  content: "text*",
  marks: "",
  group: "inline",
  inline: !0,
  selectable: !0,
  atom: !0,
  draggable: !0,
  isolating: !0,
  allowGapCursor: !1,
  priority: 1100,
  keepOnSplit: !1,
  addOptions() {
    return {
      openOnClick: !0,
      HTMLAttributes: {
        target: null,
        title: null,
        class: null,
        "data-id": null,
        "data-app-prefix": null
      },
      validate: void 0
    };
  },
  addAttributes() {
    return {
      href: {
        default: null
      },
      class: {
        default: this.options.HTMLAttributes.class
      },
      target: {
        default: this.options.HTMLAttributes.target,
        // Sanitize target value
        parseHTML: (element) => element.getAttribute("target") !== "_blank" ? null : "_blank"
      },
      title: {
        default: this.options.HTMLAttributes.title
      },
      "data-id": {
        default: this.options.HTMLAttributes["data-id"]
      },
      "data-app-prefix": {
        default: this.options.HTMLAttributes["data-app-prefix"]
      }
    };
  },
  parseHTML() {
    return [
      {
        tag: 'a[href]:not([href *= "javascript:" i])[data-id][data-app-prefix]'
      }
    ];
  },
  renderHTML({ HTMLAttributes }) {
    var _a;
    return (_a = HTMLAttributes.href) != null && _a.startsWith("javascript:") ? [
      "a",
      mergeAttributes(this.options.HTMLAttributes, {
        ...HTMLAttributes,
        href: ""
      }),
      0
    ] : [
      "a",
      mergeAttributes(this.options.HTMLAttributes, HTMLAttributes),
      0
    ];
  },
  addCommands() {
    return {
      setLinker: (attrs) => ({ commands }) => (commands.insertContent({
        type: this.name,
        attrs,
        content: [
          {
            type: "text",
            text: attrs.title
          }
        ]
      }), !0),
      unsetLinker: () => ({ state, tr }) => {
        const { node, from, to } = state.selection;
        return (node == null ? void 0 : node.type.name) === "linker" && tr.delete(from, to).scrollIntoView(), !0;
      }
    };
  }
});
export {
  Linker
};
//# sourceMappingURL=linker.js.map
