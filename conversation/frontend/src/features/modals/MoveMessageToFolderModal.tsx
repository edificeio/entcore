import { Button, Modal, Tree, TreeItem, useToast } from '@edifice.io/react';
import { useEffect, useMemo, useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useFolderActions, useI18n } from '~/hooks';
import { buildTree, useFolderUtils, useMoveMessage } from '~/services';
import { useAppActions, useSelectedMessageIds } from '~/store';

export function MoveMessageToFolderModal() {
  const { t, common_t } = useI18n();
  const { setOpenFolderModal } = useAppActions();
  const { isActionPending, foldersTree } = useFolderActions();
  const [subFolderId, setSubfolderId] = useState<string | undefined>(undefined);
  const refInputName = useRef<HTMLInputElement>(null);
  const refDropdownTrigger = useRef<HTMLButtonElement>(null);
  const selectedIds = useSelectedMessageIds();
  const moveMesage = useMoveMessage();
  const { getFolderNameById } = useFolderUtils();
  const toast = useToast();
  const navigate = useNavigate();

  useEffect(() => {
    if (isActionPending === false) setOpenFolderModal(null);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [isActionPending]);

  const handleMoveToFolderClick = () => {
    if (!subFolderId) return;
    // Mutation
    moveMesage.mutate(
      {
        folderId: subFolderId,
        id: selectedIds,
      },
      {
        onSuccess: () => {
          const folderName = getFolderNameById(subFolderId);
          const toastMessage =
            selectedIds.length > 1
              ? t('messages.move.folder', {
                  count: selectedIds.length,
                  folderName,
                })
              : t('message.move.folder', { folderName });
          toast.success(toastMessage);
          // Redirect to folder
          navigate(`/folder/${subFolderId}`);
          handleCloseFolderModal();
        },
      },
    );
  };

  const userFolders = useMemo(() => {
    return foldersTree ? buildTree(foldersTree, 2) : null;
  }, [foldersTree]);

  useEffect(() => {
    refInputName.current?.focus();
  }, []);

  if (!userFolders) return null;

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
        {t('move.first.caps')}
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
