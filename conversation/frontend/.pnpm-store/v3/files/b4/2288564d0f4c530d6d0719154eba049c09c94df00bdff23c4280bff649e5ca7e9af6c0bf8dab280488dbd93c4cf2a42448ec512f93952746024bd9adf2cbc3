import { Node } from '@tiptap/core';
export interface IframeOptions {
    allowFullscreen: boolean;
    HTMLAttributes: {
        [key: string]: any;
    };
}
declare module '@tiptap/core' {
    interface Commands<ReturnType> {
        iframe: {
            /**
             * Add an iframe
             */
            setIframe: (options: {
                src: string;
            }) => ReturnType;
        };
    }
}
export declare const Iframe: Node<IframeOptions, any>;
