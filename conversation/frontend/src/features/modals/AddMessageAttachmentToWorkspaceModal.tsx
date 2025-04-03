import { Button, Modal, WorkspaceFolders } from '@edifice.io/react';
import { useI18n } from '~/hooks';
import { useEffect, useState } from 'react';
import { useAppActions } from '~/store';

export function AddMessageAttachmentToWorkspaceModal() {
  const { setOpenFolderModal } = useAppActions();

  const { t } = useI18n();
  const [selectedFolderId, setSelectedFolderId] = useState<
    string | undefined
  >();
  const [disabled, setDisabled] = useState(false);
  const handleCloseModal = () => setOpenFolderModal(null);

  const handleFolderSelected = (folderId: string) => {
    setSelectedFolderId(folderId);
  };

  const handleAddAttachmentToWorkspace = () => {
    alert('add attachment to workspace');
  };

  // Make the button accessible when is disabled change to false
  useEffect(() => {
    setDisabled(!selectedFolderId);
  }, [selectedFolderId]);

  return (
    <Modal
      isOpen={true}
      onModalClose={handleCloseModal}
      id={'add-attachment-to-workspace-modal'}
      size="md"
    >
      <Modal.Header onModalClose={handleCloseModal}>
        {t('attachments.add.to.folder.modal')}
      </Modal.Header>
      <Modal.Body>
        <WorkspaceFolders onFolderSelected={handleFolderSelected} />
      </Modal.Body>
      <Modal.Footer>
        <Button
          type="button"
          color="tertiary"
          variant="ghost"
          onClick={handleCloseModal}
        >
          {t('cancel')}
        </Button>
        <Button
          type="submit"
          color="primary"
          variant="filled"
          onClick={handleAddAttachmentToWorkspace}
          disabled={disabled}
        >
          {t('add')}
        </Button>
      </Modal.Footer>
    </Modal>
  );
}
