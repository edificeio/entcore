import { Dropdown, IconButton, IconButtonProps } from '@edifice.io/react';
import { IconDelete, IconEdit, IconOptions } from '@edifice.io/react/icons';
import { RefAttributes } from 'react';
import { useFolderHandlers } from '~/features/menu/hooks/useFolderHandlers';
import { useI18n } from '~/hooks';
import { Folder } from '~/models';

interface FolderActionDropdownProps {
  folder: Folder;
  onDropdownOpened?: (visible: boolean) => void;
}

export function FolderActionDropdown({
  folder,
  onDropdownOpened,
}: FolderActionDropdownProps) {
  const { common_t, t } = useI18n();
  const { handleRename, handleTrash } = useFolderHandlers();
  const options = [
    // TODO: Uncomment when the action is implemented
    // {
    //   label: t('move.first.caps'),
    //   icon: <IconFolderMove />,
    //   action: handleMove,
    // },
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
    <Dropdown onToggle={onDropdownOpened}>
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
