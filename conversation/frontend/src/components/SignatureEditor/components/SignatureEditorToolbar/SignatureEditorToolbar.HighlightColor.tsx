import {
  RefAttributes,
  useCallback,
  useEffect,
  useMemo,
  useState,
} from 'react';

import { EditorInstance as Editor } from '@edifice.io/react/editor';
import {
  ColorPalette,
  ColorPicker,
  DefaultPalette,
  Dropdown,
  IconButton,
  IconButtonProps,
  Tooltip,
} from '@edifice.io/react';
import { useI18n } from '~/hooks';
import { IconTextHighlight } from '@edifice.io/react/icons';

interface SignatureEditorToolbarHighlightColorProps {
  editor: Editor;
  /**
   * Props for the trigger
   */
  triggerProps: JSX.IntrinsicAttributes &
    Omit<IconButtonProps, 'ref'> &
    RefAttributes<HTMLButtonElement>;
  /**
   * Tracks refs on ColorPickers.
   */
  itemRefs: any;
}

export const SignatureEditorToolbarHighlightColor = ({
  editor,
  triggerProps,
  itemRefs,
}: SignatureEditorToolbarHighlightColorProps) => {
  const { common_t } = useI18n();

  // Manage text and background colors.
  const [color, setColor] = useState<string>('#4A4A4A');

  const isActive = useMemo(
    () =>
      editor?.isActive('customHighlight', {
        color: /^#([0-9a-f]{3}){1,2}$/i,
      }),
    // eslint-disable-next-line react-hooks/exhaustive-deps
    [editor, editor?.state],
  );

  // Triggered when the user chooses a highlighting color.
  const applyColor = useCallback(
    (value: string) => {
      // If the same color is picked, remove it (=toggle mode).
      if (value === color || value === '') {
        setColor('');
        editor?.chain().focus().unsetHighlight().run();
      } else {
        setColor(value);
        editor?.chain().focus().setHighlight({ color: value }).run();
      }
    },
    [color, editor],
  );

  // When cursor moves in table, update the current highlight color.
  useEffect(() => {
    setColor(editor?.getAttributes('customHighlight').color ?? '');
  }, [editor, editor?.state]);

  // Palettes of available colors to choose from.
  const palettes: ColorPalette[] = [
    {
      ...DefaultPalette,
      reset: {
        value: 'transparent',
        description: common_t('tiptap.toolbar.highlight.none'),
      },
    },
  ];

  return (
    <>
      <Tooltip
        message={common_t('tiptap.toolbar.highlight.back')}
        placement="top"
      >
        <IconButton
          {...triggerProps}
          type="button"
          variant="ghost"
          color="tertiary"
          icon={<IconTextHighlight />}
          aria-label={common_t('tiptap.toolbar.highlight.back')}
          className={isActive ? 'selected' : ''}
        />
      </Tooltip>
      <Dropdown.Menu>
        <ColorPicker
          ref={(el: any) => (itemRefs.current['color-picker'] = el)}
          palettes={palettes}
          model={color}
          onSuccess={(item) => applyColor(item.value)}
        />
      </Dropdown.Menu>
    </>
  );
};
