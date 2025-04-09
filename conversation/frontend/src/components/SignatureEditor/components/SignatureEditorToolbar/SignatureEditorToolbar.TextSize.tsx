import { Fragment, RefAttributes } from 'react';
import {
  Dropdown,
  IconButton,
  IconButtonProps,
  Tooltip,
} from '@edifice.io/react';
import { EditorInstance as Editor } from '@edifice.io/react/editor';
import { IconTextSize } from '@edifice.io/react/icons';
import { useI18n } from '~/hooks';
import { useToolbarOptions } from '../../hooks';

interface SignatureEditorToolbarTextSizeProps {
  editor: Editor;
  /**
   * Props for the trigger
   */
  triggerProps: JSX.IntrinsicAttributes &
    Omit<IconButtonProps, 'ref'> &
    RefAttributes<HTMLButtonElement>;
}

export const SignatureEditorToolbarTextSize = ({
  editor,
  triggerProps,
}: SignatureEditorToolbarTextSizeProps) => {
  const { common_t } = useI18n();
  const { textOptions } = useToolbarOptions(editor);

  return (
    <>
      <Tooltip message={common_t('tiptap.toolbar.size.choice')} placement="top">
        <IconButton
          {...triggerProps}
          type="button"
          variant="ghost"
          color="tertiary"
          icon={<IconTextSize />}
          aria-label={common_t('tiptap.toolbar.size.choice')}
        />
      </Tooltip>
      <Dropdown.Menu>
        {textOptions.map((option) => {
          return (
            <Fragment key={option.id}>
              {option.type === 'divider' && option.visibility ? (
                <Dropdown.Separator />
              ) : option.visibility ? (
                <Dropdown.Item onClick={option.action}>
                  <span className={option.className}>{option.label}</span>
                </Dropdown.Item>
              ) : null}
            </Fragment>
          );
        })}
      </Dropdown.Menu>
    </>
  );
};
