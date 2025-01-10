import { useNavigate, useRouteLoaderData } from 'react-router-dom';
import { Folder } from '~/models';
import { t } from 'i18next';
import { Dropdown } from '@edifice.io/react';
import { NOOP } from '@edifice.io/utilities';
import {
  IconDelete,
  IconDepositeInbox,
  IconFolder,
  IconSend,
  IconWrite,
} from '@edifice.io/react/icons';
import { useMenuData } from '../hooks/useMenuData';

type FolderItem = { name: string; folder: Folder };

/** Convert a tree of Folders to a flat array */
function buildMenu(folders: Folder[], prefix?: string) {
  const flat: FolderItem[] = [];
  folders
    .sort((a, b) => (a.name < b.name ? -1 : a.name == b.name ? 0 : 1))
    .forEach((folder) => {
      const name = `${prefix ? prefix : ''}${folder.name}`;
      flat.push({ name, folder });
      if (folder.subFolders) {
        flat.push(...buildMenu(folder.subFolders, `${name}/`));
      }
    });
  return flat;
}

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

  const userFolders = buildMenu(foldersTree);

  const navigateTo = (systemFolderId: string) => {
    navigate(`/${systemFolderId}`);
  };

  function renderFolderItem(item: FolderItem) {
    return (
      <span className="w-100 d-flex justify-content-between align-content-center">
        <div className="overflow-x-hidden text-no-wrap text-truncate">
          {item.name}
        </div>
        {renderBadge(item.folder.nbUnread)}
      </span>
    );
  }

  return (
    <>
      <Dropdown block>
        <Dropdown.Trigger label="Dropdown" />
        <Dropdown.Menu>
          <Dropdown.Item onClick={NOOP} icon={<IconDepositeInbox />}>
            {t('inbox')}
            {renderBadge(counters.inbox)}
          </Dropdown.Item>
          <Dropdown.Item onClick={NOOP} icon={<IconSend />}>
            {t('outbox')}
          </Dropdown.Item>
          <Dropdown.Item onClick={NOOP} icon={<IconWrite />}>
            {t('draft')}
          </Dropdown.Item>
          <Dropdown.Item onClick={NOOP} icon={<IconDelete />}>
            {t('trash')}
          </Dropdown.Item>
          <Dropdown.Separator />
          <Dropdown.MenuGroup label={t('user.folders')}>
            {userFolders.map((item) => (
              <Dropdown.Item onClick={NOOP} icon={<IconFolder />}>
                {renderFolderItem(item)}
              </Dropdown.Item>
            ))}
          </Dropdown.MenuGroup>
        </Dropdown.Menu>
      </Dropdown>
    </>
  );
}
