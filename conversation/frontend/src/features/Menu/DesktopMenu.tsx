import {
  IconDelete,
  IconDepositeInbox,
  IconSend,
  IconSmiley,
  IconWrite,
} from '@edifice.io/react/icons';
import { Menu, SortableTree, TreeItem } from '@edifice.io/react';
import { NOOP } from '@edifice.io/utilities';
import { useLoaderData, useNavigate } from 'react-router-dom';
import { Folder } from '~/models';
import { t } from 'i18next';
import { useSelectedFolder } from '~/hooks';

/** Build a tree of TreeItems from Folders  */
function buildTree(folders: Folder[]) {
  return folders
    .sort((a, b) => (a.name < b.name ? -1 : a.name == b.name ? 0 : 1))
    .map(({ id, name, subFolders }) => {
      const item = { id, name } as TreeItem;
      if (subFolders) {
        item.children = buildTree(subFolders);
      }
      return item;
    });
}

export function DesktopMenu() {
  const navigate = useNavigate();
  // See `layout` loader
  const { foldersTree, actions } = useLoaderData() as {
    foldersTree: Folder[];
    actions: Record<string, boolean>;
  };
  const folder = useSelectedFolder();

  const selectedSystemFolderId =
    typeof folder === 'string' ? folder : undefined;
  const selectedUserFolderId =
    typeof folder === 'object' ? folder.name : undefined;

  if (!foldersTree || !actions) {
    return null;
  }
  const userFolders = buildTree(foldersTree);

  function renderUserFolder({
    node,
  }: {
    node: TreeItem;
    hasChildren?: boolean;
    isChild?: boolean;
  }) {
    return (
      <span
        className="w-100 d-flex justify-content-between align-content-center"
        style={{ lineHeight: '24px' }}
      >
        <div className="overflow-x-hidden text-no-wrap text-truncate">
          {node.name}
        </div>
        <IconSmiley width={20} height={24} />
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
          rightIcon={<IconSmiley />}
        >
          {t('inbox')}
        </Menu.Button>
        <Menu.Button
          selected={selectedSystemFolderId === 'outbox'}
          leftIcon={<IconSend />}
          onClick={() => navigateTo('outbox')}
          rightIcon={<IconSmiley />}
        >
          {t('outbox')}
        </Menu.Button>
        <Menu.Button
          selected={selectedSystemFolderId === 'draft'}
          leftIcon={<IconWrite />}
          onClick={() => navigateTo('draft')}
          rightIcon={<IconSmiley />}
        >
          {t('draft')}
        </Menu.Button>
        <Menu.Button
          selected={selectedSystemFolderId === 'trash'}
          leftIcon={<IconDelete />}
          onClick={() => navigateTo('trash')}
          rightIcon={<IconSmiley />}
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
