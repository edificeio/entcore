import { Node } from '@tiptap/core';
export interface AttachmentOptions {
    HTMLAttributes: Record<string, string>;
}
declare module '@tiptap/core' {
    interface Commands<ReturnType> {
        attachment: {
            setAttachment: (attachment: any) => ReturnType;
        };
    }
}
export declare const AttachmentTransformer: Node<AttachmentOptions, any>;
