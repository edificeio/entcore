import { useMemo } from 'react';

import {
  IconTextBold,
  IconTextItalic,
  IconTextVanilla,
} from '@edifice.io/react/icons';
import { useI18n } from '~/hooks/useI18n';
import { Toolbar, ToolbarItem } from '@edifice.io/react';
import { EditorInstance as Editor } from '@edifice.io/react/editor';
import { hasMark } from '../../utilities/has-mark';

interface Props {
  editorId: string;
  editor: Editor;
}

export const SignatureEditorToolbar = ({ editorId, editor }: Props) => {
  const { common_t } = useI18n();

  const toolbarItems: ToolbarItem[] = useMemo(() => {
    const showIf = (truthy: boolean) => (truthy ? 'show' : 'hide');

    return [
      //--------------- BOLD ---------------//
      {
        type: 'icon',
        props: {
          'icon': <IconTextBold />,
          'aria-label': common_t('tiptap.toolbar.bold'),
          'className': editor?.isActive('bold') ? 'is-selected' : '',
          'onClick': () => editor?.chain().focus().toggleBold().run(),
        },
        name: 'bold',
        visibility: showIf(hasMark('bold', editor)),
        tooltip: common_t('tiptap.toolbar.bold'),
      },
      //--------------- ITALIC ---------------//
      {
        type: 'icon',
        props: {
          'icon': <IconTextItalic />,
          'aria-label': common_t('tiptap.toolbar.italic'),
          'className': editor?.isActive('italic') ? 'is-selected' : '',
          'onClick': () => editor?.chain().focus().toggleItalic().run(),
        },
        name: 'italic',
        visibility: showIf(hasMark('italic', editor)),
        tooltip: common_t('tiptap.toolbar.italic'),
      },
      {
        type: 'icon',
        props: {
          'icon': <IconTextVanilla />,
          'aria-label': common_t('tiptap.toolbar.removeFormat'),
          'className': '',
          'onClick': () => editor?.chain().clearNodes().unsetAllMarks().run(),
        },
        name: 'vanilla',
        visibility: showIf(true),
        tooltip: common_t('tiptap.toolbar.removeFormat'),
      },
    ];
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [editor.state]);

  return (
    <div className="sticky-top z-1 editor-toolbar rounded-3">
      <Toolbar
        items={toolbarItems}
        variant="no-shadow"
        className="rounded-top-3"
        isBlock
        align="left"
        ariaControls={editorId}
      />
    </div>
  );
};
