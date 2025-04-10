'use strict';

Object.defineProperty(exports, '__esModule', { value: true });

var core = require('@tiptap/core');
var state = require('@tiptap/pm/state');
var view = require('@tiptap/pm/view');

/**
 * This extension allows you to add a class to the focused node.
 * @see https://www.tiptap.dev/api/extensions/focus
 */
const FocusClasses = core.Extension.create({
    name: 'focus',
    addOptions() {
        return {
            className: 'has-focus',
            mode: 'all',
        };
    },
    addProseMirrorPlugins() {
        return [
            new state.Plugin({
                key: new state.PluginKey('focus'),
                props: {
                    decorations: ({ doc, selection }) => {
                        const { isEditable, isFocused } = this.editor;
                        const { anchor } = selection;
                        const decorations = [];
                        if (!isEditable || !isFocused) {
                            return view.DecorationSet.create(doc, []);
                        }
                        // Maximum Levels
                        let maxLevels = 0;
                        if (this.options.mode === 'deepest') {
                            doc.descendants((node, pos) => {
                                if (node.isText) {
                                    return;
                                }
                                const isCurrent = anchor >= pos && anchor <= pos + node.nodeSize - 1;
                                if (!isCurrent) {
                                    return false;
                                }
                                maxLevels += 1;
                            });
                        }
                        // Loop through current
                        let currentLevel = 0;
                        doc.descendants((node, pos) => {
                            if (node.isText) {
                                return false;
                            }
                            const isCurrent = anchor >= pos && anchor <= pos + node.nodeSize - 1;
                            if (!isCurrent) {
                                return false;
                            }
                            currentLevel += 1;
                            const outOfScope = (this.options.mode === 'deepest' && maxLevels - currentLevel > 0)
                                || (this.options.mode === 'shallowest' && currentLevel > 1);
                            if (outOfScope) {
                                return this.options.mode === 'deepest';
                            }
                            decorations.push(view.Decoration.node(pos, pos + node.nodeSize, {
                                class: this.options.className,
                            }));
                        });
                        return view.DecorationSet.create(doc, decorations);
                    },
                },
            }),
        ];
    },
});

exports.FocusClasses = FocusClasses;
exports.default = FocusClasses;
//# sourceMappingURL=index.cjs.map
