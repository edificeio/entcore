/** Our own model of an hyperlink in a rich document. */
export type HyperlinkAttributes = {
    href: string | null;
    target: '_blank' | null;
    title: string | null;
    text: string | null;
};
declare module '@tiptap/core' {
    interface Commands<ReturnType> {
        hyperlink: {
            /**
             * Set an hyperlink mark
             */
            setLink: (attributes: Partial<HyperlinkAttributes>) => ReturnType;
            /**
             * Toggle an hyperlink mark
             */
            toggleLink: (attributes: {
                href: string;
                target?: string | null;
            }) => ReturnType;
            /**
             * Unset an hyperlink mark
             */
            unsetLink: () => ReturnType;
        };
    }
}
/**
 * Hyperlink (external links), extends `Link` extension from TipTap.
 *
 * Links to external resources MUST NOT have a `data-id` nor a `data-app-prefix` attribute.
 * The `target` attribute has to be sanitized, so it is overriden.
 */
export declare const Hyperlink: import('@tiptap/core').Mark<import('@tiptap/extension-link').LinkOptions, any>;
