import { Node } from '@tiptap/core';
export interface SpeechSynthesisOptions {
    lang: string;
    pitch: number;
}
declare module '@tiptap/core' {
    interface Commands<ReturnType> {
        speechSynthesis: {
            startSpeechSynthesis: () => ReturnType;
            stopSpeechSynthesis: () => ReturnType;
        };
    }
}
declare class SS_Node<O = any, S = any> extends Node<O, S> {
    static create<O = any, S = any>(config?: any): SS_Node<O, S>;
}
export declare const SpeechSynthesis: SS_Node<SpeechSynthesisOptions, any>;
export {};
