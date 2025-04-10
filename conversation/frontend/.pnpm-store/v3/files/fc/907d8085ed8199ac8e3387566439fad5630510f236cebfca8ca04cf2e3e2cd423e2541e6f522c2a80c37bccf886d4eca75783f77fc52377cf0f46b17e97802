import { TableCell as TableCell$1 } from "@tiptap/extension-table-cell";
const TableCell = TableCell$1.extend({
  addAttributes() {
    var _a;
    return {
      ...(_a = this.parent) == null ? void 0 : _a.call(this),
      backgroundColor: {
        default: null,
        renderHTML: (attributes) => attributes.backgroundColor ? {
          style: `background-color: ${attributes.backgroundColor}`
        } : {},
        parseHTML: (element) => {
          var _a2, _b;
          return (_b = (_a2 = element.style) == null ? void 0 : _a2.backgroundColor) == null ? void 0 : _b.replace(/['"]+/g, "");
        }
      }
    };
  }
});
export {
  TableCell
};
//# sourceMappingURL=table-cell.js.map
