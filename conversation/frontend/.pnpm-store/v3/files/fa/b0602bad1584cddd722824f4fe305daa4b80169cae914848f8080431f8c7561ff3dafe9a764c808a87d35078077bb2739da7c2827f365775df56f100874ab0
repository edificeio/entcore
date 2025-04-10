(function (global, factory) {
  typeof exports === 'object' && typeof module !== 'undefined' ? factory(exports, require('@tiptap/extension-text-style'), require('@tiptap/core')) :
  typeof define === 'function' && define.amd ? define(['exports', '@tiptap/extension-text-style', '@tiptap/core'], factory) :
  (global = typeof globalThis !== 'undefined' ? globalThis : global || self, factory(global["@tiptap/extension-font-family"] = {}, null, global.core));
})(this, (function (exports, extensionTextStyle, core) { 'use strict';

  /**
   * This extension allows you to set a font family for text.
   * @see https://www.tiptap.dev/api/extensions/font-family
   */
  const FontFamily = core.Extension.create({
      name: 'fontFamily',
      addOptions() {
          return {
              types: ['textStyle'],
          };
      },
      addGlobalAttributes() {
          return [
              {
                  types: this.options.types,
                  attributes: {
                      fontFamily: {
                          default: null,
                          parseHTML: element => element.style.fontFamily,
                          renderHTML: attributes => {
                              if (!attributes.fontFamily) {
                                  return {};
                              }
                              return {
                                  style: `font-family: ${attributes.fontFamily}`,
                              };
                          },
                      },
                  },
              },
          ];
      },
      addCommands() {
          return {
              setFontFamily: fontFamily => ({ chain }) => {
                  return chain()
                      .setMark('textStyle', { fontFamily })
                      .run();
              },
              unsetFontFamily: () => ({ chain }) => {
                  return chain()
                      .setMark('textStyle', { fontFamily: null })
                      .removeEmptyTextStyle()
                      .run();
              },
          };
      },
  });

  exports.FontFamily = FontFamily;
  exports.default = FontFamily;

  Object.defineProperty(exports, '__esModule', { value: true });

}));
//# sourceMappingURL=index.umd.js.map
