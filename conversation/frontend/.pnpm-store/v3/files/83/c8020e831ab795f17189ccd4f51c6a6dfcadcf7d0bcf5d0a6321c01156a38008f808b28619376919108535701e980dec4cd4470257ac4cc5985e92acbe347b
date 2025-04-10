import '@tiptap/extension-text-style';
import { Extension } from '@tiptap/core';

/**
 * This extension allows you to set a font family for text.
 * @see https://www.tiptap.dev/api/extensions/font-family
 */
const FontFamily = Extension.create({
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

export { FontFamily, FontFamily as default };
//# sourceMappingURL=index.js.map
