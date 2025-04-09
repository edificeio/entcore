import { EditorInstance } from '@edifice.io/react/editor';
import { DropdownMenuOptions } from '@edifice.io/react';
import { IconBulletList, IconOrderedList } from '@edifice.io/react/icons';
import { useI18n } from '~/hooks';
import { hasExtension } from '../utilities/has-extension';

export const useToolbarOptions = (editor: EditorInstance | null) => {
  const { common_t } = useI18n();

  const listOptions: DropdownMenuOptions[] = [
    {
      icon: <IconBulletList />,
      label: common_t('tiptap.toolbar.ulist'),
      action: () => editor?.chain().focus().toggleBulletList().run(),
    },
    {
      icon: <IconOrderedList />,
      label: common_t('tiptap.toolbar.olist'),
      action: () => editor?.chain().focus().toggleOrderedList().run(),
    },
  ];

  const textOptions = [
    {
      id: 'title-1',
      label: common_t('tiptap.toolbar.size.h1'),
      className: 'fs-2 fw-bold text-secondary',
      action: () =>
        editor?.chain().focus().setCustomHeading({ level: 1 }).run(),
      visibility: hasExtension('customHeading', editor),
    },
    {
      id: 'title-2',
      label: common_t('tiptap.toolbar.size.h2'),
      className: 'fs-3 fw-bold text-secondary',
      action: () =>
        editor?.chain().focus().setCustomHeading({ level: 2 }).run(),
      visibility: hasExtension('customHeading', editor),
    },
    {
      id: 'divider',
      type: 'divider',
      visibility:
        hasExtension('customHeading', editor) &&
        hasExtension('fontSize', editor),
    },
    {
      id: 'big-text',
      label: common_t('tiptap.toolbar.size.big'),
      className: 'fs-4',
      action: () =>
        editor?.chain().focus().setParagraph().setFontSize('18px').run(),
      visibility: hasExtension('fontSize', editor),
    },
    {
      id: 'normal-text',
      label: common_t('tiptap.toolbar.size.normal'),
      action: () =>
        editor?.chain().focus().setParagraph().setFontSize('16px').run(),
      visibility: hasExtension('fontSize', editor),
    },
    {
      id: 'small-text',
      label: common_t('tiptap.toolbar.size.small'),
      className: 'fs-6',
      action: () =>
        editor?.chain().focus().setParagraph().setFontSize('14px').run(),
      visibility: hasExtension('fontSize', editor),
    },
  ];

  /* TODO not currently exported from front framework
  const alignmentOptions: DropdownMenuOptions[] = [
    {
      icon: <IconAlignLeft />,
      label: common_t('tiptap.toolbar.text.left'),
      action: () => editor?.chain().focus().setTextAlign('left').run(),
    },
    {
      icon: <IconAlignCenter />,
      label: common_t('tiptap.toolbar.text.center'),
      action: () => editor?.chain().focus().setTextAlign('center').run(),
    },
    {
      icon: <IconAlignRight />,
      label: common_t('tiptap.toolbar.text.right'),
      action: () => editor?.chain().focus().setTextAlign('right').run(),
    },
    {
      icon: <IconAlignJustify />,
      label: common_t('tiptap.toolbar.text.justify'),
      action: () => editor?.chain().focus().setTextAlign('justify').run(),
    },
  ];
  */
  return { listOptions, textOptions /*, alignmentOptions*/ };
};
