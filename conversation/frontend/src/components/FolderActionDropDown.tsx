import { Dropdown, IconButton, IconButtonProps } from '@edifice.io/react';
import {
  IconDelete,
  IconEdit,
  IconFolderMove,
  IconOptions,
} from '@edifice.io/react/icons';
import { RefAttributes } from 'react';
import { useFolderHandlers } from '~/features/menu/hooks/useFolderHandlers';
import { useI18n } from '~/hooks';
import { Folder } from '~/models';

export function FolderActionDropDown({ folder }: { folder: Folder }) {
  const { common_t, t } = useI18n();
  const { handleMove, handleRename, handleTrash } = useFolderHandlers();

  const options = [
    {
      label: t('move.first.caps'),
      icon: <IconFolderMove />,
      action: handleMove,
    },
    {
      label: t('rename'),
      icon: <IconEdit />,
      action: handleRename,
    },
    {
      label: t('delete'),
      icon: <IconDelete />,
      action: handleTrash,
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
              <Dropdown.Item
                key={option.label}
                icon={option.icon}
                onClick={() => option.action(folder)}
              >
                {option.label}
              </Dropdown.Item>
            ))}
          </Dropdown.Menu>
        </div>
      )}
    </Dropdown>
  );
}
