import { Node } from '@tiptap/core';
export interface VideoOptions {
    url: string;
    width: number;
    height: number;
    HTMLAttributes: Record<string, any>;
}
declare module '@tiptap/core' {
    interface Commands<ReturnType> {
        video: {
            /**
             * Set a video node
             * @param options.updateSelection set to true will select the newly inserted content
             */
            setVideo: (id: string, src: string, isCaptation: boolean, width?: number, height?: number, controls?: boolean, controlslist?: string, options?: {
                updateSelection: boolean;
            }) => ReturnType;
            /**
             * Toggle a video
             */
            toggleVideo: (src: string) => ReturnType;
        };
    }
}
export declare const Video: Node<any, any>;
