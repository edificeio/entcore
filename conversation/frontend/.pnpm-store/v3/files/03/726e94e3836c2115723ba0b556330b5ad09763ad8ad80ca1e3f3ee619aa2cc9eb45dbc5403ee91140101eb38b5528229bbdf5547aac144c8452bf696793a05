import '@tiptap/extension-text-style';
import { Extension } from '@tiptap/core';
export type FontFamilyOptions = {
    /**
     * A list of node names where the font family can be applied.
     * @default ['textStyle']
     * @example ['heading', 'paragraph']
     */
    types: string[];
};
declare module '@tiptap/core' {
    interface Commands<ReturnType> {
        fontFamily: {
            /**
             * Set the font family
             * @param fontFamily The font family
             * @example editor.commands.setFontFamily('Arial')
             */
            setFontFamily: (fontFamily: string) => ReturnType;
            /**
             * Unset the font family
             * @example editor.commands.unsetFontFamily()
             */
            unsetFontFamily: () => ReturnType;
        };
    }
}
/**
 * This extension allows you to set a font family for text.
 * @see https://www.tiptap.dev/api/extensions/font-family
 */
export declare const FontFamily: Extension<FontFamilyOptions, any>;
//# sourceMappingURL=font-family.d.ts.map