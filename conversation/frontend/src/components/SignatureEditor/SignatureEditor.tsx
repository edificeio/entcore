import {
  EditorContent,
  Content,
  useEditor,
  StarterKit,
} from '@edifice.io/react/editor';
import { useI18n } from '~/hooks';

export function SignatureEditor(
  editable: boolean,
  content: Content,
  placeholder?: string,
  onContentChange?: ({ editor }: { editor: any }) => void,
) {
  const { common_t } = useI18n();

  const editor = useEditor({
    editable: true,
    extensions: [
      StarterKit,
      Placeholder.configure({
        placeholder: t(placeholder || 'tiptap.placeholder'),
      }),
      CustomHighlight.configure({
        multicolor: true,
      }),
      Underline,
      TextStyle,
      Color,
      TextAlign.configure({
        types: ['heading', 'paragraph', 'video', 'audio'],
      }),
      CustomHeading.configure({
        levels: [1, 2],
      }),
      Typography,
      FontSize,
      Hyperlink,
      FontFamily,
      Alert,
    ],
    content,
    // If the onContentChange callback is provided, we call it on every content change.
    ...(onContentChange
      ? {
          onUpdate: onContentChange,
        }
      : {}),
  });

  useEffect(() => {
    editor?.setEditable(editable, false); // Don't emit the update event, since content did not change.
  }, [editor, editable]);

  useEffect(() => {
    editor?.commands.setContent(content);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [content]);

  useEffect(() => {
    focus && editor?.commands.focus(focus);
  }, [editor, focus, editable]);

  return { editor, editable };
}
