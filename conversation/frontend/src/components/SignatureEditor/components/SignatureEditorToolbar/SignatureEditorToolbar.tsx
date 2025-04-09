import { RefAttributes, useMemo } from 'react';

import {
  IconBulletList,
  IconTextBold,
  IconTextItalic,
  IconTextVanilla,
} from '@edifice.io/react/icons';
import { useI18n } from '~/hooks';
import { IconButtonProps, Toolbar, ToolbarItem } from '@edifice.io/react';
import { EditorInstance as Editor } from '@edifice.io/react/editor';
import { hasMark } from '../../utilities/has-mark';
import { hasExtension } from '../../utilities/has-extension';
import { SignatureEditorToolbarDropdownMenu } from './SignatureEditorToolbar.DropdownMenu';
import { SignatureEditorToolbarHighlightColor } from './SignatureEditorToolbar.HighlightColor';
import { useToolbarOptions } from '../../hooks';
import { SignatureEditorToolbarTextSize } from './SignatureEditorToolbar.TextSize';

interface Props {
  editorId: string;
  editor: Editor;
}

export const SignatureEditorToolbar = ({ editorId, editor }: Props) => {
  const { common_t } = useI18n();

  const { listOptions /*, alignmentOptions*/ } = useToolbarOptions(editor);

  const toolbarItems: ToolbarItem[] = useMemo(() => {
    const showIf = (truthy: boolean) => (truthy ? 'show' : 'hide');

    return [
      //--------------- TEXT SIZE ---------------//
      {
        type: 'dropdown',
        props: {
          children: (
            triggerProps: JSX.IntrinsicAttributes &
              Omit<IconButtonProps, 'ref'> &
              RefAttributes<HTMLButtonElement>,
          ) => (
            <SignatureEditorToolbarTextSize
              editor={editor}
              triggerProps={triggerProps}
            />
          ),
        },
        name: 'text_size',
        visibility: showIf(
          (hasExtension('textStyle', editor) &&
            hasExtension('fontSize', editor)) ||
            hasExtension('heading', editor),
        ),
        tooltip: common_t('tiptap.toolbar.size.choice'),
      },
      //--------------- TEXT HIGHLIGHTING COLOR ---------------//
      {
        type: 'dropdown',
        props: {
          children: (
            triggerProps: JSX.IntrinsicAttributes &
              Omit<IconButtonProps, 'ref'> &
              RefAttributes<HTMLButtonElement>,
            itemRefs: any,
          ) => (
            <SignatureEditorToolbarHighlightColor
              editor={editor}
              triggerProps={triggerProps}
              itemRefs={itemRefs}
            />
          ),
        },
        name: 'highlight',
        visibility: showIf(hasMark('customHighlight', editor)),
        tooltip: common_t('tiptap.toolbar.highlight.back'),
      },
      //--------------- BOLD ---------------//
      {
        type: 'icon',
        props: {
          'icon': <IconTextBold />,
          'aria-label': common_t('tiptap.toolbar.bold'),
          'className': editor?.isActive('bold') ? 'is-selected' : '',
          'onClick': () => editor?.chain().focus().toggleBold().run(),
          'disabled': editor?.isActive('heading'),
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
      //--------------- UNDERLINE ---------------//
      /* TODO not currently exported from front framework
      {
        type: 'icon',
        props: {
          'icon': <IconTextUnderline />,
          'aria-label': common_t('tiptap.toolbar.underline'),
          'className': editor?.isActive('underline') ? 'is-selected' : '',
          'onClick': () => editor?.chain().focus().toggleUnderline().run(),
        },
        name: 'underline',
        visibility: showIf(hasMark('underline', editor)),
        tooltip: common_t('tiptap.toolbar.underline'),
      },
      */
      //--------------- LIST MENU ---------------//
      {
        type: 'dropdown',
        props: {
          children: (
            triggerProps: JSX.IntrinsicAttributes &
              Omit<IconButtonProps, 'ref'> &
              RefAttributes<HTMLButtonElement>,
          ) => (
            <SignatureEditorToolbarDropdownMenu
              triggerProps={triggerProps}
              icon={<IconBulletList />}
              ariaLabel={common_t('tiptap.toolbar.listoptions')}
              options={listOptions}
            />
          ),
        },
        name: 'list',
        visibility: showIf(hasExtension('starterKit', editor)),
        tooltip: common_t('tiptap.toolbar.listoptions'),
      },
      //--------------- TEXT ALIGNMENT ---------------//
      /* TODO not currently exported from front framework
      {
        type: 'dropdown',
        props: {
          children: (
            triggerProps: JSX.IntrinsicAttributes &
              Omit<IconButtonProps, 'ref'> &
              RefAttributes<HTMLButtonElement>,
          ) => (
            <SignatureEditorToolbarDropdownMenu
              triggerProps={triggerProps}
              icon={<IconAlignLeft />}
              ariaLabel={common_t('tiptap.toolbar.align')}
              options={alignmentOptions}
            />
          ),
        },
        name: 'alignment',
        visibility: showIf(hasExtension('textAlign', editor)),
        tooltip: common_t('tiptap.toolbar.align'),
      },
      */
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
  }, [common_t, editor, listOptions]);

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
