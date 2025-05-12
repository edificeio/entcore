import {
  Button,
  Menu,
  Tree,
  TreeItem,
  useEdificeClient,
} from '@edifice.io/react';
import {
  IconDelete,
  IconDepositeInbox,
  IconPlus,
  IconSend,
  IconWrite,
} from '@edifice.io/react/icons';
import { useMemo, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { useNavigate } from 'react-router-dom';
import {
  FolderActionDropdown,
  ProgressBar,
  ProgressBarProps,
} from '~/components';
import { useUsedSpace } from '~/hooks';
import { buildTree, useFoldersTree } from '~/services';
import { useFolderHandlers } from '../hooks/useFolderHandlers';
import { useMenuData } from '../hooks/useMenuData';
import './DesktopMenu.css';
import clsx from 'clsx';

/** Converts a value in bytes to mega-bytes (rounded) */
const bytesToMegabytes = (bytes: number) => Math.round(bytes / (1024 * 1024));

/** The navigation menu among folders, intended for desktop resolutions */
export function DesktopMenu() {
  const navigate = useNavigate();
  const foldersTreeQuery = useFoldersTree();
  const { appCode } = useEdificeClient();
  const { t } = useTranslation(appCode);
  const { t: common_t } = useTranslation('common');
  const {
    counters,
    renderBadge,
    selectedSystemFolderId,
    selectedUserFolderId,
  } = useMenuData();

  const { usage, quota } = useUsedSpace();

  const { handleCreate: handleNewFolderClick } = useFolderHandlers();

  const userFolders = useMemo(() => {
    const foldersTree = foldersTreeQuery.data;
    return foldersTree ? buildTree(foldersTree) : null;
  }, [foldersTreeQuery]);

  const progress = quota > 0 ? (usage * 100) / quota : 0;

  if (!userFolders) {
    return null;
  }

  const progressBarProps: ProgressBarProps = {
    label:
      quota > 0
        ? `${bytesToMegabytes(usage)} / ${bytesToMegabytes(quota)} ${common_t('mb')}`
        : '',
    progress: progress,
    labelOptions: {
      justify: 'end',
    },
    progressOptions: {
      color: progress < 70 ? 'info' : progress < 90 ? 'warning' : 'danger',
    },
  };

  const navigateTo = (folderId: string, isUserFolder = false) => {
    navigate((isUserFolder ? '/folder/' : '/') + folderId);
  };

  // Render a user's folder, to be used in a Tree or SortableTree
  function renderUserFolder({ node }: { node: TreeItem }) {
    const [dropdownOpened, setDropdownOpened] = useState(false);
    const handleDropdownOpened = (visible: boolean) => {
      setDropdownOpened(visible);
    };

    return (
      <div
        className={clsx(
          'folder-item my-n8 py-2 w-100 d-flex justify-content-between align-content-center align-items-center',
          { 'dropdown-opened': dropdownOpened },
        )}
      >
        <div className="overflow-x-hidden text-no-wrap text-truncate">
          {node.name}
        </div>
        <div className="d-flex align-items-center text-dark fw-normal justify-content-center">
          <div className="unread-badge">
            {renderBadge(node.folder.nbUnread)}
          </div>
          <div className="actions-button">
            <FolderActionDropdown
              folder={node.folder}
              onDropdownOpened={handleDropdownOpened}
            />
          </div>
        </div>
      </div>
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
        >
          {t('trash')}
        </Menu.Button>
      </Menu.Item>
      <Menu.Item>
        <div className="w-100 border-bottom pt-8 mb-12"></div>
      </Menu.Item>
      <Menu.Item>
        <div className="d-flex flex-column">
          <strong className="fs-6 mb-8">{t('user.folders')}</strong>
          <Tree
            nodes={userFolders}
            onTreeItemClick={(folderId) => navigateTo(folderId, true)}
            renderNode={renderUserFolder}
            selectedNodeId={selectedUserFolderId}
          />
          <Button
            type="button"
            color="secondary"
            variant="ghost"
            size="sm"
            leftIcon={<IconPlus />}
            className="d-inline-flex"
            onClick={handleNewFolderClick}
          >
            {common_t('workspace.folder.create')}
          </Button>
        </div>
      </Menu.Item>
      <Menu.Item>
        <div className="w-100 border-bottom pt-8 mb-12"></div>
      </Menu.Item>
      <Menu.Item>
        <div className="d-flex flex-column gap-8">
          <b className="fs-6">{t('used.space')}</b>
          <ProgressBar {...progressBarProps} />
        </div>
      </Menu.Item>
    </Menu>
  );
}

const Ok = () => {
  return <div>OK</div>;
};
