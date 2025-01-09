import {
  IconDelete,
  IconDepositeInbox,
  IconSend,
  IconWrite,
} from '@edifice.io/react/icons';
import { Badge, Menu, SortableTree, TreeItem } from '@edifice.io/react';
import { NOOP } from '@edifice.io/utilities';
import { useNavigate, useRouteLoaderData } from 'react-router-dom';
import { Folder } from '~/models';
import { t } from 'i18next';
import { useSelectedFolder } from '~/hooks';
import { useMessagesCount } from '~/services';
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
  const { folderId, userFolder } = useSelectedFolder();

  const inboxCount =
    useMessagesCount('inbox', { unread: true }).data?.count ?? 0;
  const outboxCount =
    useMessagesCount('outbox', { unread: true }).data?.count ?? 0;
  const draftCount = useMessagesCount('draft').data?.count ?? 0;
  const trashCount =
    useMessagesCount('trash', { unread: true }).data?.count ?? 0;

  const selectedSystemFolderId =
    typeof folderId === 'string' && !userFolder ? folderId : undefined;
  const selectedUserFolderId =
    typeof userFolder === 'object' ? folderId : undefined;

  if (!foldersTree || !actions) {
    return null;
  }
  const userFolders = buildTree(foldersTree);

  function renderBadge(count: number) {
    return (
      <>
        {count > 0 && (
          <Badge
            variant={{
              level: 'warning',
              type: 'notification',
            }}
          >
            {count}
          </Badge>
        )}
      </>
    );
  }

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

  const navigateTo = (systemFolderId: string) => {
    navigate(`/${systemFolderId}`);
  };

  return (
    <Menu label={t('generic.folders')}>
      <Menu.Item>
        <Menu.Button
          selected={selectedSystemFolderId === 'inbox'}
          leftIcon={<IconDepositeInbox />}
          onClick={() => navigateTo('inbox')}
          rightIcon={renderBadge(inboxCount)}
        >
          {t('inbox')}
        </Menu.Button>
        <Menu.Button
          selected={selectedSystemFolderId === 'outbox'}
          leftIcon={<IconSend />}
          onClick={() => navigateTo('outbox')}
          rightIcon={renderBadge(outboxCount)}
        >
          {t('outbox')}
        </Menu.Button>
        <Menu.Button
          selected={selectedSystemFolderId === 'draft'}
          leftIcon={<IconWrite />}
          onClick={() => navigateTo('draft')}
          rightIcon={renderBadge(draftCount)}
        >
          {t('draft')}
        </Menu.Button>
        <Menu.Button
          selected={selectedSystemFolderId === 'trash'}
          leftIcon={<IconDelete />}
          onClick={() => navigateTo('trash')}
          rightIcon={renderBadge(trashCount)}
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
