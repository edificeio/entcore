import { useNavigate, useRouteLoaderData } from 'react-router-dom';
import { Folder, SystemFolder } from '~/models';
import { t } from 'i18next';
import { Dropdown } from '@edifice.io/react';
import {
  IconDelete,
  IconDepositeInbox,
  IconFolder,
  IconSend,
  IconWrite,
} from '@edifice.io/react/icons';
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
  folders
    .sort((a, b) => (a.name < b.name ? -1 : a.name == b.name ? 0 : 1))
    .forEach((folder) => {
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
          `${name}/`,
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
  return {
    name: t(folder),
    folder: {
      id: folder,
      depth: 1,
      nbUnread: counters[folder],
      name: t(folder),
      trashed: false,
      nbMessages: 0,
    },
  };
}

/** The folder navigation menu, in mobile responsive mode. */
export function MobileMenu() {
  const navigate = useNavigate();
  const { foldersTree, actions } = useRouteLoaderData('layout') as {
    foldersTree: Folder[];
    actions: Record<string, boolean>;
  };
  const {
    counters,
    renderBadge,
    selectedSystemFolderId,
    selectedUserFolderId,
  } = useMenuData();

  if (!foldersTree || !actions) {
    return null;
  }

  const menu = buildMenuItemsWithSelection(selectedUserFolderId, foldersTree);
  let { selectedItem } = menu;

  const systemMenuItems = {
    inbox: asFolderItem('inbox', counters),
    outbox: asFolderItem('outbox', counters),
    draft: asFolderItem('draft', counters),
    trash: asFolderItem('trash', counters),
  };
  if (!selectedItem && selectedSystemFolderId) {
    selectedItem = systemMenuItems[selectedSystemFolderId];
  }

  function renderFolderItem(item: FolderItem) {
    return (
      <div className="w-100 d-flex justify-content-between align-content-center align-items-center">
        <div className="overflow-x-hidden text-no-wrap text-truncate">
          {item.name}
        </div>
        {renderBadge(item.folder.nbUnread)}
      </div>
    );
  }

  function handleClickItem(item: FolderItem, isUserFolder = false) {
    navigate(`${isUserFolder ? '/folder/' : '/'}${item.folder.id}`);
  }

  return (
    <Dropdown block>
      <Dropdown.Trigger label={selectedItem?.name || selectedUserFolderId} />
      <Dropdown.Menu>
        <Dropdown.Item
          onClick={() => handleClickItem(systemMenuItems.inbox)}
          icon={<IconDepositeInbox />}
        >
          {renderFolderItem(systemMenuItems.inbox)}
        </Dropdown.Item>
        <Dropdown.Item
          onClick={() => handleClickItem(systemMenuItems.outbox)}
          icon={<IconSend />}
        >
          {renderFolderItem(systemMenuItems.outbox)}
        </Dropdown.Item>
        <Dropdown.Item
          onClick={() => handleClickItem(systemMenuItems.draft)}
          icon={<IconWrite />}
        >
          {renderFolderItem(systemMenuItems.draft)}
        </Dropdown.Item>
        <Dropdown.Item
          onClick={() => handleClickItem(systemMenuItems.trash)}
          icon={<IconDelete />}
        >
          {renderFolderItem(systemMenuItems.trash)}
        </Dropdown.Item>
        <Dropdown.Separator />
        <Dropdown.MenuGroup label={t('user.folders')}>
          {menu.menuItems.map((item) => (
            <Dropdown.Item
              onClick={() => handleClickItem(item, true)}
              icon={<IconFolder />}
            >
              {renderFolderItem(item)}
            </Dropdown.Item>
          ))}
        </Dropdown.MenuGroup>
      </Dropdown.Menu>
    </Dropdown>
  );
}
