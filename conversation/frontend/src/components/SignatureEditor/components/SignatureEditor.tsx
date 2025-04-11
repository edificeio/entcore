import {
  forwardRef,
  Ref,
  useEffect,
  useId,
  useImperativeHandle,
  useState,
} from 'react';

import { FontSize } from '@edifice.io/tiptap-extensions/font-size';
import {
  EditorContent,
  Content,
  useEditor,
  StarterKit,
  EditorInstance,
} from '@edifice.io/react/editor';
import { SignatureEditorToolbar } from './SignatureEditorToolbar';
import clsx from 'clsx';

export interface SignatureEditorRef {
  /** Get the current content as HTML. */
  getHtmlContent: () => undefined | string;
}

export interface SignatureEditorProps {
  content: Content;
  mode: 'edit' | 'read';
  placeholder: string;
  maxLength?: number;
}

export const SignatureEditor = forwardRef(
  (
    { content, mode = 'read', maxLength = 800 }: SignatureEditorProps,
    ref: Ref<SignatureEditorRef>,
  ) => {
    const id = useId();

    const [size, setSize] = useState<number>(0);

    function updateCounter(editor: EditorInstance) {
      const text = editor.getText({ blockSeparator: '' });
      if (typeof text === 'string') {
        // Check if maxLength is reached
        setSize(text.length);
      }
    }

    const editor = useEditor({
      extensions: [
        StarterKit.configure({
          heading: false,
          blockquote: false,
          listItem: false,
          orderedList: false,
          strike: false,
          bulletList: false,
          code: false,
        }),
        FontSize,
      ],
      content,
      editable: true,
      onUpdate: (props) => updateCounter(props.editor),
      editorProps: {
        handleTextInput: (view) => {
          if (
            view.state.doc.textContent.length >= maxLength &&
            view.state.selection.empty
          ) {
            return true;
          }
        },
        handlePaste: (view, _event, slice) => {
          if (view.state.doc.textContent.length + slice.size > maxLength) {
            return true;
          }
        },
      },
    });

    useImperativeHandle(ref, () => ({
      getHtmlContent: () => editor?.getHTML(),
    }));

    useEffect(() => {
      editor?.setEditable(mode === 'edit', false);
    }, [editor, mode]);

    useEffect(() => {
      if (editor) {
        editor.commands.setContent(content);
        updateCounter(editor);
      }
      // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [content]);

    if (editor === null) return null;

    const borderClass = clsx('border rounded-3');
    const contentClass = clsx('py-12 px-16');

    return (
      <>
        <div className={borderClass}>
          <SignatureEditorToolbar editorId={id} editor={editor} />
          <EditorContent id={id} editor={editor} className={contentClass} />
        </div>
        <p className="small text-gray-700 p-2 text-end">
          <i>{`${size} / ${maxLength}`}</i>
        </p>
      </>
    );
  },
);
