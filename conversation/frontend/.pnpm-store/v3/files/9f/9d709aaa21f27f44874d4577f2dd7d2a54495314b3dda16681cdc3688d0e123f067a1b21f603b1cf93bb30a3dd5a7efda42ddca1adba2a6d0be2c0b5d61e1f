import { Node } from '@tiptap/core';
export interface SpeechRecognitionOptions {
    lang: string;
}
declare module '@tiptap/core' {
    interface Commands<ReturnType> {
        SpeechRecognition: {
            startSpeechRecognition: () => ReturnType;
            stopSpeechRecognition: () => ReturnType;
            isSpeechRecognitionStarted: () => boolean;
        };
    }
}
declare class SR_Node<O = any, S = any> extends Node<O, S> {
    protected constructor();
    recognition: SpeechRecognition | undefined;
    readonly isStarted: boolean;
    static create<O = any, S = any>(config?: any): SR_Node<O, S>;
}
export declare const SpeechRecognition: SR_Node<SpeechRecognitionOptions, any>;
export {};
