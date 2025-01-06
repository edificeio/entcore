import {
  IconDelete,
  IconDepositeInbox,
  IconSend,
  IconSmiley,
  IconWrite,
} from '@edifice.io/react/icons';
import { Button, Menu, SortableTree, TreeItem } from '@edifice.io/react';
import { NOOP } from '@edifice.io/utilities';
import { useLoaderData } from 'react-router-dom';
import { Folder } from '~/models';
import { t } from 'i18next';

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
  // See `layout` loader
  const { foldersTree, actions } = useLoaderData() as {
    foldersTree: Folder[];
    actions: Record<string, boolean>;
  };

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
      <div className="w-100 d-flex">
        {node.name}
        <IconSmiley />
      </div>
    );
  }

  return (
    <>
      <Menu label={t('generic.folders')}>
        <Menu.Item>
          <Menu.Button
            leftIcon={<IconDepositeInbox />}
            onClick={NOOP}
            rightIcon={<IconSmiley />}
          >
            {t('inbox')}
          </Menu.Button>
          <Menu.Button
            leftIcon={<IconSend />}
            onClick={NOOP}
            rightIcon={<IconSmiley />}
          >
            {t('outbox')}
          </Menu.Button>
          <Menu.Button
            leftIcon={<IconWrite />}
            onClick={NOOP}
            rightIcon={<IconSmiley />}
          >
            {t('draft')}
          </Menu.Button>
          <Menu.Button
            leftIcon={<IconDelete />}
            onClick={NOOP}
            rightIcon={<IconSmiley />}
          >
            {t('trash')}
          </Menu.Button>
        </Menu.Item>
      </Menu>
      <div className="w-100 border-bottom"></div>
      <Menu label={t('user.folders')}>
        <b>{t('user.folders')}</b>
        <SortableTree
          nodes={userFolders}
          onSortable={NOOP}
          onTreeItemClick={NOOP}
          renderNode={renderUserFolder}
          selectedNodeId={'1'}
        />
      </Menu>
      <div className="w-100 border-bottom"></div>
      TODO Espace utilisé
    </>
  );
}
