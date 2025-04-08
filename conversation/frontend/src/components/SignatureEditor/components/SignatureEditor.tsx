import { forwardRef, Ref, useEffect, useId, useImperativeHandle } from 'react';

import { FontSize } from '@edifice.io/tiptap-extensions/font-size';
import { CustomHeading } from '@edifice.io/tiptap-extensions/heading';
import { CustomHighlight } from '@edifice.io/tiptap-extensions/highlight';
import { Hyperlink } from '@edifice.io/tiptap-extensions/hyperlink';
import {
  EditorContent,
  Content,
  useEditor,
  StarterKit,
} from '@edifice.io/react/editor';
import { SignatureEditorToolbar } from './SignatureEditorToolbar';
import clsx from 'clsx';

export interface SignatureEditorRef {
  /** Get the current content as HTML. */
  getHtmlContent: () => undefined | string;
}

export interface EditorProps {
  content: Content;
  mode: 'edit' | 'read';
  placeholder: string;
}

export const SignatureEditor = forwardRef(
  ({ content, mode = 'read' }: EditorProps, ref: Ref<SignatureEditorRef>) => {
    const id = useId();
    const editor = useEditor({
      extensions: [
        StarterKit.configure({
          heading: false,
        }),
        CustomHighlight.configure({
          multicolor: true,
        }),
        CustomHeading.configure({
          levels: [1, 2],
        }),
        FontSize,
        Hyperlink,
      ],
      content,
      editable: true,
    });

    useImperativeHandle(ref, () => ({
      getHtmlContent: () => editor?.getHTML(),
    }));

    useEffect(() => {
      editor?.setEditable(mode === 'edit', false);
    }, [editor, mode]);

    useEffect(() => {
      editor?.commands.setContent(content);
      // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [content]);

    if (editor === null) return null;

    const borderClass = clsx('border rounded-3');
    const contentClass = clsx('py-12 px-16');

    return (
      <div className={borderClass}>
        {mode === 'edit' && (
          <SignatureEditorToolbar editorId={id} editor={editor} />
        )}
        <EditorContent id={id} editor={editor} className={contentClass} />
      </div>
    );
  },
);
