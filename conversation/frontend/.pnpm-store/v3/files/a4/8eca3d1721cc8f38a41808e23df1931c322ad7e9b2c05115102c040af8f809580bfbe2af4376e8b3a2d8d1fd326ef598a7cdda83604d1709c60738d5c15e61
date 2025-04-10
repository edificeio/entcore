import { Node } from '@tiptap/core';
export type LinkerAttributes = {
    'href': string | null;
    'target': '_blank' | null;
    'title': string | null;
    'data-id': string | null;
    'data-app-prefix': string | null;
};
declare module '@tiptap/core' {
    interface Commands<ReturnType> {
        linker: {
            /**
             * Set a linker node
             */
            setLinker: (attributes: LinkerAttributes) => ReturnType;
            /**
             * Unset a linker node
             */
            unsetLinker: () => ReturnType;
        };
    }
}
/**
 * Internal links extension.
 * Reproduces the legacy angularJs "linker" directive.
 *
 * Links to internal resources MAY have a `title` and MUST HAVE `data-id` and `data-app-prefix` attributes :
 * `<a href="/blog#/view/35fa4198-blog_id/5e654c71-article_id" data-app-prefix="blog" data-id="35fa4198-blog_id" target="_blank" title="Voir ce billet de blog" class="ng-scope">/blog#/view/35fa4198-57fe-45eb-94f4-a5e4defff305/5e654c71-1e61-4f84-86dc-6fcfaf33f513</a>`
 */
export declare const Linker: Node<any, any>;
