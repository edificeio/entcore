import { Node } from '@tiptap/core';
export interface AudioOptions {
    url: string;
    HTMLAttributes: Record<string, any>;
}
declare module '@tiptap/core' {
    interface Commands<ReturnType> {
        audio: {
            /**
             * Set a audio node
             * @param options.updateSelection set to true will select the newly inserted content
             */
            setAudio: (id: string, src: string, options?: {
                updateSelection: boolean;
            }) => ReturnType;
        };
    }
}
export declare const Audio: Node<any, any>;
