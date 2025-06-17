import { Dropdown } from '@edifice.io/react';
import {
  IconDelete,
  IconDepositeInbox,
  IconFolder,
  IconPlus,
  IconSend,
  IconWrite,
} from '@edifice.io/react/icons';
import { t } from 'i18next';
import { ReactElement } from 'react';
import { useNavigate } from 'react-router-dom';
import { FolderActionDropdown } from '~/components';
import { useI18n } from '~/hooks/useI18n';
import { Folder, SystemFolder } from '~/models';
import { useFoldersTree } from '~/services';
import { useFolderHandlers } from '../hooks/useFolderHandlers';
import { useMenuData } from '../hooks/useMenuData';

type FolderItem = { name: string; folder: Folder };

/**
 * Convert a tree of Folders to a flat array of FolderItems,
 * and get a reference to the currently selected item.
 */
function buildMenuItemsWithSelection(
  selectedFolderId: string | undefined,
  folders: Folder[],
  prefix?: string,
) {
  const items: FolderItem[] = [];
  let selectedItem: FolderItem | undefined;
  folders.forEach((folder) => {
    // Build an item representing this folder
    const name = `${prefix ? prefix : ''}${folder.name}`;
    const item = { name, folder };
    items.push(item);

    // Is this the selected folder ?
    if (selectedFolderId === folder.id) {
      selectedItem = item;
    }

    // Recursively build items for subFolders
    if (folder.subFolders) {
      const subs = buildMenuItemsWithSelection(
        selectedFolderId,
        folder.subFolders,
        `${name} / `,
      );
      items.push(...subs.menuItems);
      if (subs.selectedItem) {
        selectedItem = subs.selectedItem;
      }
    }
  });
  return { menuItems: items, selectedItem };
}

/** Build a FolderItem representing a system folder. */
function asFolderItem(
  folder: SystemFolder,
  counters: {
    inbox: number;
    outbox: number;
    draft: number;
    trash: number;
  },
) {
  const folderName = t(`${folder}.folder`);
  return {
    name: folderName,
    key: folder,
    folder: {
      id: folder,
      parent_id: null,
      depth: 1,
      nbUnread: counters[folder],
      name: folderName,
      nbMessages: 0,
    },
  };
}

/** The folder navigation menu, in mobile responsive mode. */
export function MobileMenu() {
  const navigate = useNavigate();
  const foldersTreeQuery = useFoldersTree();
  const { t } = useI18n();

  const {
    counters,
    renderBadge,
    selectedSystemFolderId,
    selectedUserFolderId,
  } = useMenuData();
  const { handleCreate: handleNewFolderClick } = useFolderHandlers();

  if (!foldersTreeQuery.data) {
    return null;
  }

  const menu = buildMenuItemsWithSelection(
    selectedUserFolderId,
    foldersTreeQuery.data,
  );
  let { selectedItem } = menu;

  const systemFolderItems = Array.of<{
    name: SystemFolder;
    icon: ReactElement;
  }>(
    { name: 'inbox', icon: <IconDepositeInbox /> },
    { name: 'outbox', icon: <IconSend /> },
    { name: 'draft', icon: <IconWrite /> },
    { name: 'trash', icon: <IconDelete /> },
  ).map((item) => ({
    ...item,
    ...asFolderItem(item.name, counters),
    onClick: handleItemClick,
  }));

  if (!selectedItem && selectedSystemFolderId) {
    selectedItem = systemFolderItems.filter(
      (item) => item.key === selectedSystemFolderId,
    )?.[0];
  }

  function renderFolderItem(item: FolderItem, isUserFolder = false) {
    return (
      <div className="w-100 d-flex justify-content-between align-content-center align-items-center">
        <div>{item.name}</div>
        {isUserFolder ? (
          <div className="d-flex align-items-center gap-4">
            {renderBadge(item.folder.nbUnread)}
            <FolderActionDropdown folder={item.folder} />
          </div>
        ) : (
          renderBadge(item.folder.nbUnread)
        )}
      </div>
    );
  }

  function handleItemClick(item: FolderItem, isUserFolder = false) {
    navigate(`${isUserFolder ? '/folder/' : '/'}${item.folder.id}`);
  }

  return (
    <div className="position-relative">
      <Dropdown block>
        <Dropdown.Trigger
          label={(selectedItem && selectedItem.name) || selectedUserFolderId}
        />
        <Dropdown.Menu>
          {systemFolderItems.map((item) => (
            <Dropdown.Item
              onClick={() => item.onClick(item)}
              key={item.name}
              icon={item.icon}
            >
              {renderFolderItem(item)}
            </Dropdown.Item>
          ))}
          <Dropdown.Separator />
          <Dropdown.MenuGroup label={t('user.folders')}>
            {menu.menuItems.map((item) => (
              <Dropdown.Item
                key={item.name}
                onClick={() => handleItemClick(item, true)}
                icon={<IconFolder />}
              >
                {renderFolderItem(item, true)}
              </Dropdown.Item>
            ))}
            <Dropdown.Item
              className="text-secondary"
              onClick={handleNewFolderClick}
              icon={<IconPlus />}
            >
              {t('new.folder')}
            </Dropdown.Item>
          </Dropdown.MenuGroup>
        </Dropdown.Menu>
      </Dropdown>
    </div>
  );
}
