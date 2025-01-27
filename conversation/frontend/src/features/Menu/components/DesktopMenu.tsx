import {
  IconDelete,
  IconDepositeInbox,
  IconPlus,
  IconSend,
  IconWrite,
} from '@edifice.io/react/icons';
import {
  Button,
  Menu,
  SortableTree,
  TreeItem,
  useEdificeClient,
} from '@edifice.io/react';
import { NOOP } from '@edifice.io/utilities';
import { useNavigate } from 'react-router-dom';
import { Folder } from '~/models';
import { useMenuData } from '../hooks/useMenuData';
import './DesktopMenu.css';
import {
  FolderActionDropDown,
  ProgressBar,
  ProgressBarProps,
} from '~/components';
import { useTranslation } from 'react-i18next';
import { useUsedSpace } from '~/hooks';
import { useFoldersTree } from '~/store';
import { useFolderHandlers } from '../hooks/useFolderHandlers';

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
/** Converts a value in bytes to mega-bytes (rounded) */
const bytesToMegabytes = (bytes: number) => Math.round(bytes / (1024 * 1024));

/** The navigation menu among folders, intended for desktop resolutions */
export function DesktopMenu() {
  const navigate = useNavigate();
  const foldersTree = useFoldersTree();
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

  if (!foldersTree) {
    return null;
  }

  const userFolders = buildTree(foldersTree);
  const progress = quota > 0 ? (usage * 100) / quota : 0;

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

  // Render a user's folder, to be used in a SortableTree
  function renderUserFolder({
    node,
  }: {
    node: TreeItem;
    hasChildren?: boolean;
    isChild?: boolean;
  }) {
    return (
      <div className="user-folder w-100 d-flex justify-content-between align-content-center align-items-center">
        <div className="overflow-x-hidden text-no-wrap text-truncate">
          {node.name}
        </div>
        <div className="d-flex align-items-center">
          {renderBadge(node.folder)}
          <FolderActionDropDown folder={node.folder} />
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
        <div className="d-flex flex-column">
          <b className="fs-6 mb-8">{t('user.folders')}</b>
          <SortableTree
            nodes={userFolders}
            onSortable={NOOP}
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
            {t('new.folder')}
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
