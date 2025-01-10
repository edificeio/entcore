import {
  IconDelete,
  IconDepositeInbox,
  IconSend,
  IconWrite,
} from '@edifice.io/react/icons';
import { Menu, SortableTree, TreeItem } from '@edifice.io/react';
import { NOOP } from '@edifice.io/utilities';
import { useNavigate, useRouteLoaderData } from 'react-router-dom';
import { Folder } from '~/models';
import { useMenuData } from '../hooks/useMenuData';
import { t } from 'i18next';
import './DesktopMenu.css';

type FolderTreeItem = TreeItem & { folder: Folder };

/** Convert a tree of Folders to custom TreeItems  */
function buildTree(folders: Folder[]) {
  return folders
    .sort((a, b) => (a.name < b.name ? -1 : a.name == b.name ? 0 : 1))
    .map((folder) => {
      const item = {
        id: folder.id,
        name: folder.name,
        folder,
      } as FolderTreeItem;
      if (folder.subFolders) {
        item.children = buildTree(folder.subFolders);
      }
      return item;
    });
}

/** The navigation menu among folders, intended for desktop resolutions */
export function DesktopMenu() {
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

  const userFolders = buildTree(foldersTree);

  const navigateTo = (systemFolderId: string) => {
    navigate(`/${systemFolderId}`);
  };

  // Render a user's folder, to be used in a SortableTree
  function renderUserFolder({
    node,
  }: {
    node: TreeItem;
    hasChildren?: boolean;
    isChild?: boolean;
  }) {
    return (
      <span className="user-folder w-100 d-flex justify-content-between align-content-center">
        <div className="overflow-x-hidden text-no-wrap text-truncate">
          {node.name}
        </div>
        {renderBadge(node.folder)}
      </span>
    );
  }

  return (
    <Menu label={t('generic.folders')}>
      <Menu.Item>
        <Menu.Button
          selected={selectedSystemFolderId === 'inbox'}
          leftIcon={<IconDepositeInbox />}
          onClick={() => navigateTo('inbox')}
          rightIcon={renderBadge(counters.inbox)}
        >
          {t('inbox')}
        </Menu.Button>
        <Menu.Button
          selected={selectedSystemFolderId === 'outbox'}
          leftIcon={<IconSend />}
          onClick={() => navigateTo('outbox')}
          rightIcon={renderBadge(counters.outbox)}
        >
          {t('outbox')}
        </Menu.Button>
        <Menu.Button
          selected={selectedSystemFolderId === 'draft'}
          leftIcon={<IconWrite />}
          onClick={() => navigateTo('draft')}
          rightIcon={renderBadge(counters.draft)}
        >
          {t('draft')}
        </Menu.Button>
        <Menu.Button
          selected={selectedSystemFolderId === 'trash'}
          leftIcon={<IconDelete />}
          onClick={() => navigateTo('trash')}
          rightIcon={renderBadge(counters.trash)}
        >
          {t('trash')}
        </Menu.Button>
      </Menu.Item>
      <Menu.Item>
        <div className="w-100 border-bottom pt-8 mb-12"></div>
      </Menu.Item>
      <Menu.Item>
        <b>{t('user.folders')}</b>
        <SortableTree
          nodes={userFolders}
          onSortable={NOOP}
          onTreeItemClick={(folderId) => navigateTo(folderId)}
          renderNode={renderUserFolder}
          selectedNodeId={selectedUserFolderId}
        />
      </Menu.Item>
      <Menu.Item>
        <div className="w-100 border-bottom pt-8 mb-12"></div>
      </Menu.Item>
      <Menu.Item>TODO Espace utilisé</Menu.Item>
    </Menu>
  );
}
