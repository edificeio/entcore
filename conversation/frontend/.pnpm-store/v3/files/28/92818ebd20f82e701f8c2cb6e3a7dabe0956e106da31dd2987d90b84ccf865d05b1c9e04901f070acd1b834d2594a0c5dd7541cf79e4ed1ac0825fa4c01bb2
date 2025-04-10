import { Node } from '@tiptap/core';
export interface AttachmentOptions {
    HTMLAttributes: Record<string, string>;
}
declare module '@tiptap/core' {
    interface Commands<ReturnType> {
        attachment: {
            setAttachment: (attachment: any) => ReturnType;
            unsetAttachment: (documentId: string) => ReturnType;
        };
    }
}
export declare const Attachment: Node<AttachmentOptions, any>;
