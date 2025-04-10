import { WorkspaceElement } from '@edifice.io/client';
export declare const IMAGE_INPUT_REGEX: RegExp;
export interface CustomImageOptions {
    HTMLAttributes: Record<string, string>;
    sizes: string[];
    uploadFile?: (file: File) => Promise<WorkspaceElement | null>;
}
interface AttributesProps {
    width: number | string;
    height: number | string;
    size: string;
}
declare module '@tiptap/core' {
    interface Commands<ReturnType> {
        customImage: {
            setAttributes: (options: AttributesProps) => ReturnType;
            setNewImage: (options: {
                src: string;
                alt?: string;
                title?: string;
            }) => ReturnType;
        };
    }
}
export declare const Image: import('@tiptap/core').Node<CustomImageOptions, any>;
export {};
