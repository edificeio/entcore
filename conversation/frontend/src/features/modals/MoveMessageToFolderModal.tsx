import { useAppActions, useSelectedMessageIds } from '~/store';
import {
  Button,
  Modal,
  Tree,
  TreeItem,
} from '@edifice.io/react';
import { useEffect, useMemo, useRef, useState } from 'react';
import { useFolderActions, useI18n } from '~/hooks';
import { buildTree, useMoveMessage } from '~/services';

export function MoveMessageToFolderModal() {
  const { t, common_t } = useI18n();
  const { setOpenFolderModal } = useAppActions();
  const { isActionPending, foldersTree } = useFolderActions();
  const [subFolderId, setSubfolderId] = useState<string | undefined>(undefined);
  const refInputName = useRef<HTMLInputElement>(null);
  const refDropdownTrigger = useRef<HTMLButtonElement>(null);
  const selectedIds = useSelectedMessageIds();
  const moveMesage = useMoveMessage();

  useEffect(() => {
    if (isActionPending === false) setOpenFolderModal(null);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [isActionPending]);

  const handleMoveToFolderClick = () => {
    if(!subFolderId) return;
    moveMesage.mutate({
      folderId: subFolderId,
      id: selectedIds
    });
    handleCloseFolderModal();
  };

  const userFolders = useMemo(() => {
    return foldersTree ? buildTree(foldersTree, 2) : null;
  }, [foldersTree]);

  useEffect(() => {
    refInputName.current?.focus();
  }, []);

  if (!userFolders) return <></>;

  // Render a user's folder, to be used in a Tree
  const renderFolderTreeItem = ({
    node,
  }: {
    node: TreeItem;
    hasChildren?: boolean;
    isChild?: boolean;
  }) => <span>{node.name}</span>;

  const handleCloseFolderModal = () => setOpenFolderModal(null);
  const handleTreeItemClick = (folderId: string) => {
    setSubfolderId(folderId);
    // Close dropdown
    refDropdownTrigger.current?.click();
  };

  return (
    <Modal
      size="sm"
      id="modalFolderNew"
      isOpen={true}
      onModalClose={handleCloseFolderModal}
    >
      <Modal.Header onModalClose={handleCloseFolderModal}>
        {t('move')}
      </Modal.Header>

      <Modal.Body>
        {userFolders.length > 0 && (
          <>
            <Tree
              nodes={userFolders}
              onTreeItemClick={handleTreeItemClick}
              renderNode={renderFolderTreeItem}
              selectedNodeId={subFolderId}
              shouldExpandAllNodes={true}
              showIcon={false}
            />
          </>
        )}
      </Modal.Body>

      <Modal.Footer>
        <Button
          type="button"
          color="tertiary"
          variant="ghost"
          onClick={handleCloseFolderModal}
        >
          {common_t('cancel')}
        </Button>
        <Button
          type="submit"
          color="primary"
          variant="filled"
          onClick={handleMoveToFolderClick}
          isLoading={isActionPending === true}
          disabled={isActionPending === true || !subFolderId}
        >
          {t('move')}
        </Button>
      </Modal.Footer>
    </Modal>
  );
}
