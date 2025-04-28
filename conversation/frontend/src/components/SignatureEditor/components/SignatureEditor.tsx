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
  onLengthChange?: (isValid: boolean, newLength: number) => void;
}

export const SignatureEditor = forwardRef(
  (
    {
      content,
      mode = 'read',
      maxLength = 800,
      onLengthChange,
    }: SignatureEditorProps,
    ref: Ref<SignatureEditorRef>,
  ) => {
    const id = useId();
    const [length, setLength] = useState<number>(0);

    function updateCounter(editor: EditorInstance) {
      const text = editor.getText({ blockSeparator: '' });
      if (typeof text === 'string') {
        setLength(text.length);
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

    useEffect(() => {
      onLengthChange?.(length <= maxLength, length);
    }, [length, maxLength, onLengthChange]);

    if (editor === null) return null;

    const counterClass = clsx(
      'small p-2 text-end',
      length > maxLength ? 'text-danger' : 'text-gray-700',
    );

    return (
      <>
        <div className={'border rounded-3'}>
          <SignatureEditorToolbar editorId={id} editor={editor} />
          <EditorContent id={id} editor={editor} className={'py-12 px-16'} />
        </div>
        <p className={counterClass}>
          <i>{`${length} / ${maxLength}`}</i>
        </p>
      </>
    );
  },
);
