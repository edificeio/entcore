import { Node } from '@tiptap/core';
export interface ImageOptions {
    /**
     * Controls if the image node should be inline or not.
     * @default false
     * @example true
     */
    inline: boolean;
    /**
     * Controls if base64 images are allowed. Enable this if you want to allow
     * base64 image urls in the `src` attribute.
     * @default false
     * @example true
     */
    allowBase64: boolean;
    /**
     * HTML attributes to add to the image element.
     * @default {}
     * @example { class: 'foo' }
     */
    HTMLAttributes: Record<string, any>;
}
declare module '@tiptap/core' {
    interface Commands<ReturnType> {
        image: {
            /**
             * Add an image
             * @param options The image attributes
             * @example
             * editor
             *   .commands
             *   .setImage({ src: 'https://tiptap.dev/logo.png', alt: 'tiptap', title: 'tiptap logo' })
             */
            setImage: (options: {
                src: string;
                alt?: string;
                title?: string;
            }) => ReturnType;
        };
    }
}
/**
 * Matches an image to a ![image](src "title") on input.
 */
export declare const inputRegex: RegExp;
/**
 * This extension allows you to insert images.
 * @see https://www.tiptap.dev/api/nodes/image
 */
export declare const Image: Node<ImageOptions, any>;
//# sourceMappingURL=image.d.ts.map