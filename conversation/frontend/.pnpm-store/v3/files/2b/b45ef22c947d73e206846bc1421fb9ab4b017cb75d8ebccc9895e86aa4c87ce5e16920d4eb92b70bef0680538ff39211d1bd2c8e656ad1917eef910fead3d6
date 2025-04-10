import { Link } from "@tiptap/extension-link";
const Hyperlink = Link.extend({
  name: "hyperlink",
  parseHTML() {
    return [
      {
        tag: 'a[href]:not([href *= "javascript:" i])',
        // Be sure no data-id and data-app-prefix attribute exists :
        // it would then be an Linker, not an Hyperlink !
        getAttrs: (node) => {
          if (node.getAttribute("data-id") && node.getAttribute("data-app-prefix"))
            return !1;
        }
      }
    ];
  },
  addOptions() {
    var _a, _b;
    return {
      ...(_a = this.parent) == null ? void 0 : _a.call(this),
      openOnClick: !1,
      HTMLAttributes: {
        ...(_b = this.parent) == null ? void 0 : _b.call(this).HTMLAttributes,
        target: null
      }
    };
  },
  /* Manage `title` and `target` attributes. */
  addAttributes() {
    var _a;
    return {
      // Preserve attributes of parent extension...
      ...(_a = this.parent) == null ? void 0 : _a.call(this),
      // ...then add or override the following :
      //------------------
      target: {
        default: this.options.HTMLAttributes.target,
        // Sanitize target value
        parseHTML: (element) => element.getAttribute("target") !== "_blank" ? null : "_blank",
        renderHTML: (attributes) => ({
          target: attributes.target
        })
      },
      title: {
        default: this.options.HTMLAttributes.title
      }
    };
  }
});
export {
  Hyperlink
};
//# sourceMappingURL=hyperlink.js.map
