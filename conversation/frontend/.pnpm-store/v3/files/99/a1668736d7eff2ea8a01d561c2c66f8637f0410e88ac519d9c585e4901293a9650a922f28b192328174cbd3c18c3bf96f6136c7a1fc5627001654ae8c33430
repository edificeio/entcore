import { Node } from "@tiptap/core";
const MathJax = Node.create({
  name: "mathjaxnode",
  group: "inline",
  inline: !0,
  atom: !1,
  selectable: !0,
  parseHTML() {
    return [
      {
        tag: "mathjax"
      }
    ];
  },
  addAttributes() {
    return {
      equation: {
        default: null,
        parseHTML: (element) => {
          const textNodes = [...element.childNodes].filter((child) => child.nodeType === 3);
          return textNodes.length > 0 ? textNodes[textNodes.length - 1].nodeValue : null;
        }
      }
    };
  },
  renderHTML({ HTMLAttributes }) {
    let equation = (HTMLAttributes.equation || "").replaceAll(
      /(?:\\)?begin{equation}\s*\n?\s*{(.+?)}\n?\s*?(?:\\)?end{equation}/gm,
      "$$$1$$"
    ).replaceAll(
      /(?:\\)?begin{equation}\s*\n?\s*(.+?)\n?\s*?(?:\\)?end{equation}/gm,
      "$$$1$$"
    );
    return equation.length > 0 && (equation.charAt(0) !== "$" && (equation = `$${equation}`), equation.charAt(equation.length - 1) !== "$" && (equation = `${equation}$`)), ["span", {}, equation];
  }
});
export {
  MathJax
};
//# sourceMappingURL=mathjax.js.map
