import { Dropdown, IconButton, IconButtonProps } from '@edifice.io/react';
import {
  IconDelete,
  IconEdit,
  IconFolderMove,
  IconOptions,
} from '@edifice.io/react/icons';
import { Fragment, RefAttributes } from 'react';
import { useFolderActions } from '~/features/Menu/hooks/useFolderActions';
import { useTranslation } from 'react-i18next';
import { Folder } from '~/models';

export function FolderActionDropDown({ folder }: { folder: Folder }) {
  const { t: common_t } = useTranslation('common');
  const { handleMove, handleRename, handleDelete } = useFolderActions();

  const options = [
    {
      label: common_t('move'),
      icon: <IconFolderMove />,
      action: handleMove,
    },
    {
      label: common_t('rename'),
      icon: <IconEdit />,
      action: handleRename,
    },
    {
      label: common_t('delete'),
      icon: <IconDelete />,
      action: handleDelete,
    },
  ];

  return (
    <Dropdown>
      {(
        triggerProps: JSX.IntrinsicAttributes &
          Omit<IconButtonProps, 'ref'> &
          RefAttributes<HTMLButtonElement>,
      ) => (
        <div data-testid="dropdown">
          <IconButton
            {...triggerProps}
            className="border-0"
            type="button"
            size="sm"
            aria-label={common_t('tiptap.tooltip.plus')}
            color="tertiary"
            variant="ghost"
            icon={<IconOptions />}
          />

          <Dropdown.Menu>
            {options.map((option) => (
              <Fragment key={option.label}>
                <Dropdown.Item
                  icon={option.icon}
                  onClick={() => option.action(folder)}
                >
                  {option.label}
                </Dropdown.Item>
              </Fragment>
            ))}
          </Dropdown.Menu>
        </div>
      )}
    </Dropdown>
  );
}
