export declare type Level = 1 | 2;
interface Options {
    levels: Level[];
    HTMLAttributes: Record<string, any>;
}
declare module '@tiptap/core' {
    interface Commands<ReturnType> {
        customHeading: {
            /**
             * Apply Heading Level
             */
            setCustomHeading: (attributes: {
                level: Level;
            }) => ReturnType;
        };
    }
}
export declare const CustomHeading: import('@tiptap/core').Node<Options, any>;
export {};
