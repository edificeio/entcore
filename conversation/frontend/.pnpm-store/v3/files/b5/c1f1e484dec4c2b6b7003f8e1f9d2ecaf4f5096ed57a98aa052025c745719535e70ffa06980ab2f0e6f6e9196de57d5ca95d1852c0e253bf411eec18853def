(function (global, factory) {
  typeof exports === 'object' && typeof module !== 'undefined' ? factory(exports, require('@tiptap/core'), require('@tiptap/pm/state')) :
  typeof define === 'function' && define.amd ? define(['exports', '@tiptap/core', '@tiptap/pm/state'], factory) :
  (global = typeof globalThis !== 'undefined' ? globalThis : global || self, factory(global["@tiptap/extension-character-count"] = {}, global.core, global.state));
})(this, (function (exports, core, state) { 'use strict';

  /**
   * This extension allows you to count the characters and words of your document.
   * @see https://tiptap.dev/api/extensions/character-count
   */
  const CharacterCount = core.Extension.create({
      name: 'characterCount',
      addOptions() {
          return {
              limit: null,
              mode: 'textSize',
              textCounter: text => text.length,
              wordCounter: text => text.split(' ').filter(word => word !== '').length,
          };
      },
      addStorage() {
          return {
              characters: () => 0,
              words: () => 0,
          };
      },
      onBeforeCreate() {
          this.storage.characters = options => {
              const node = (options === null || options === void 0 ? void 0 : options.node) || this.editor.state.doc;
              const mode = (options === null || options === void 0 ? void 0 : options.mode) || this.options.mode;
              if (mode === 'textSize') {
                  const text = node.textBetween(0, node.content.size, undefined, ' ');
                  return this.options.textCounter(text);
              }
              return node.nodeSize;
          };
          this.storage.words = options => {
              const node = (options === null || options === void 0 ? void 0 : options.node) || this.editor.state.doc;
              const text = node.textBetween(0, node.content.size, ' ', ' ');
              return this.options.wordCounter(text);
          };
      },
      addProseMirrorPlugins() {
          let initialEvaluationDone = false;
          return [
              new state.Plugin({
                  key: new state.PluginKey('characterCount'),
                  appendTransaction: (transactions, oldState, newState) => {
                      if (initialEvaluationDone) {
                          return;
                      }
                      const limit = this.options.limit;
                      if (limit === null || limit === undefined || limit === 0) {
                          initialEvaluationDone = true;
                          return;
                      }
                      const initialContentSize = this.storage.characters({ node: newState.doc });
                      if (initialContentSize > limit) {
                          const over = initialContentSize - limit;
                          const from = 0;
                          const to = over;
                          console.warn(`[CharacterCount] Initial content exceeded limit of ${limit} characters. Content was automatically trimmed.`);
                          const tr = newState.tr.deleteRange(from, to);
                          initialEvaluationDone = true;
                          return tr;
                      }
                      initialEvaluationDone = true;
                  },
                  filterTransaction: (transaction, state) => {
                      const limit = this.options.limit;
                      // Nothing has changed or no limit is defined. Ignore it.
                      if (!transaction.docChanged || limit === 0 || limit === null || limit === undefined) {
                          return true;
                      }
                      const oldSize = this.storage.characters({ node: state.doc });
                      const newSize = this.storage.characters({ node: transaction.doc });
                      // Everything is in the limit. Good.
                      if (newSize <= limit) {
                          return true;
                      }
                      // The limit has already been exceeded but will be reduced.
                      if (oldSize > limit && newSize > limit && newSize <= oldSize) {
                          return true;
                      }
                      // The limit has already been exceeded and will be increased further.
                      if (oldSize > limit && newSize > limit && newSize > oldSize) {
                          return false;
                      }
                      const isPaste = transaction.getMeta('paste');
                      // Block all exceeding transactions that were not pasted.
                      if (!isPaste) {
                          return false;
                      }
                      // For pasted content, we try to remove the exceeding content.
                      const pos = transaction.selection.$head.pos;
                      const over = newSize - limit;
                      const from = pos - over;
                      const to = pos;
                      // It’s probably a bad idea to mutate transactions within `filterTransaction`
                      // but for now this is working fine.
                      transaction.deleteRange(from, to);
                      // In some situations, the limit will continue to be exceeded after trimming.
                      // This happens e.g. when truncating within a complex node (e.g. table)
                      // and ProseMirror has to close this node again.
                      // If this is the case, we prevent the transaction completely.
                      const updatedSize = this.storage.characters({ node: transaction.doc });
                      if (updatedSize > limit) {
                          return false;
                      }
                      return true;
                  },
              }),
          ];
      },
  });

  exports.CharacterCount = CharacterCount;
  exports.default = CharacterCount;

  Object.defineProperty(exports, '__esModule', { value: true });

}));
//# sourceMappingURL=index.umd.js.map
