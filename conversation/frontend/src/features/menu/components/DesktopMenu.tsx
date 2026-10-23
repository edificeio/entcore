import { useDndContext, useDroppable } from '@dnd-kit/core';
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
import clsx from 'clsx';
import { ReactNode, useMemo, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { useNavigate } from 'react-router-dom';
import {
  FolderActionDropdown,
  ProgressBar,
  ProgressBarProps,
} from '~/components';
import { useCanDropMessagesOn } from '~/features/message-list/components/MessageDragAndDrop';
import { useUsedSpace } from '~/hooks/useUsedSpace';
import { buildTree, useFoldersTree } from '~/services';
import { useFolderHandlers } from '../hooks/useFolderHandlers';
import { useMenuData } from '../hooks/useMenuData';
import { useUnfoldOnDragHover } from '../hooks/useUnfoldOnDragHover';
import './DesktopMenu.css';
import { DesktopMenuSkeleton } from './DesktopMenuSkeleton';

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
  const canDropMessagesOn = useCanDropMessagesOn();
  const { active, over } = useDndContext();

  // Folder currently hovered by dragged messages, even if they can't be dropped there.
  const overFolderId = active
    ? (over?.data.current?.id as string | undefined)
    : undefined;
  // Same folder, only when the dragged messages can be dropped there.
  const dropFolderId =
    overFolderId && canDropMessagesOn(overFolderId) ? overFolderId : undefined;
  useUnfoldOnDragHover(overFolderId);

  const userFolders = useMemo(() => {
    const foldersTree = foldersTreeQuery.data;
    return foldersTree ? buildTree(foldersTree) : null;
  }, [foldersTreeQuery]);

  const progress = quota > 0 ? (usage * 100) / quota : 0;

  if (foldersTreeQuery.isPending) {
    return <DesktopMenuSkeleton />;
  }

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
    navigate((isUserFolder ? '/folder/' : '/') + folderId, {
      state: { scrollToTop: true },
    });
  };

  return (
    <Menu label={t('generic.folders')}>
      <Menu.Item>
        <SystemFolderDropZone folderId="inbox">
          <Menu.Button
            selected={selectedSystemFolderId === 'inbox'}
            leftIcon={<IconDepositeInbox />}
            onClick={() => navigateTo('inbox')}
            rightIcon={renderBadge(counters.inbox)}
            className={clsx({ 'drop-target': dropFolderId === 'inbox' })}
          >
            {t('inbox.folder')}
          </Menu.Button>
        </SystemFolderDropZone>
        <SystemFolderDropZone folderId="outbox">
          <Menu.Button
            selected={selectedSystemFolderId === 'outbox'}
            leftIcon={<IconSend />}
            onClick={() => navigateTo('outbox')}
            className={clsx({ 'drop-target': dropFolderId === 'outbox' })}
          >
            {t('outbox.folder')}
          </Menu.Button>
        </SystemFolderDropZone>
        <SystemFolderDropZone folderId="draft">
          <Menu.Button
            selected={selectedSystemFolderId === 'draft'}
            leftIcon={<IconWrite />}
            onClick={() => navigateTo('draft')}
            rightIcon={renderBadge(counters.draft)}
            className={clsx({ 'drop-target': dropFolderId === 'draft' })}
          >
            {t('draft.folder')}
          </Menu.Button>
        </SystemFolderDropZone>
        <SystemFolderDropZone folderId="trash">
          <Menu.Button
            selected={selectedSystemFolderId === 'trash'}
            leftIcon={<IconDelete />}
            onClick={() => navigateTo('trash')}
            className={clsx({ 'drop-target': dropFolderId === 'trash' })}
          >
            {t('trash.folder')}
          </Menu.Button>
        </SystemFolderDropZone>
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
            renderNode={({ node }) => (
              <UserFolderItem
                node={node}
                isDropTarget={dropFolderId === node.id}
              />
            )}
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

/** Makes a system folder of the menu a drop target for dragged messages. */
function SystemFolderDropZone({
  folderId,
  children,
}: {
  folderId: string;
  children: ReactNode;
}) {
  const { setNodeRef } = useDroppable({
    id: `${folderId}-drop-zone`,
    data: { id: folderId },
  });
  return (
    <div ref={setNodeRef} data-testid={`conversation-dropzone-${folderId}`}>
      {children}
    </div>
  );
}

function UserFolderItem({
  node,
  isDropTarget,
}: {
  node: TreeItem;
  isDropTarget: boolean;
}) {
  const { renderBadge } = useMenuData();
  // Every folder row, sub-folders included, is a drop target for dragged messages.
  // (The DndTree of @edifice.io/react only registers root folders.)
  const { setNodeRef } = useDroppable({
    id: `folder-drop-zone-${node.id}`,
    data: { id: node.id },
  });
  const [dropdownOpened, setDropdownOpened] = useState(false);
  const handleDropdownOpened = (visible: boolean) => {
    setDropdownOpened(visible);
  };

  return (
    <div
      ref={setNodeRef}
      className={clsx(
        'folder-item my-n8 py-2 w-100 d-flex justify-content-between align-content-center align-items-center',
        { 'dropdown-opened': dropdownOpened, 'drop-target': isDropTarget },
      )}
    >
      <div
        className="overflow-x-hidden text-no-wrap text-truncate"
        title={node.name}
      >
        {node.name}
      </div>
      <div className="d-flex align-items-center text-dark fw-normal justify-content-center">
        <div className="unread-badge">{renderBadge(node.folder.nbUnread)}</div>
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
