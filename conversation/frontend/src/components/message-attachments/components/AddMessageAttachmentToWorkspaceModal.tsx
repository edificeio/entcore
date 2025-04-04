import { Button, Modal } from '@edifice.io/react';
import { WorkspaceFolders } from '@edifice.io/react/multimedia';
import { useEffect, useState } from 'react';
import { createPortal } from 'react-dom';
import { useI18n } from '~/hooks';
import { useMessageAttachments } from '~/hooks/useMessageAttachments';
import { Message } from '~/models';

interface AddMessageAttachmentToWorkspaceModalProps {
  message: Message;
  attachmentId: string;
  onModalClose: () => void;
  isOpen?: boolean;
}

export function AddMessageAttachmentToWorkspaceModal({
  message,
  attachmentId,
  isOpen = false,
  onModalClose,
}: AddMessageAttachmentToWorkspaceModalProps) {
  const { t } = useI18n();
  const { copyToWorkspace } = useMessageAttachments(message);
  const [selectedFolderId, setSelectedFolderId] = useState<
    string | undefined
  >();
  const [isLoading, setIsLoading] = useState(false);
  const [disabled, setDisabled] = useState(false);

  const handleFolderSelected = (folderId: string) => {
    setSelectedFolderId(folderId);
  };

  const handleAddAttachmentToWorkspace = async () => {
    if (!selectedFolderId) return;
    setIsLoading(true);
    const isSuccess = await copyToWorkspace(attachmentId, selectedFolderId);
    if (isSuccess) {
      onModalClose();
    }
    setIsLoading(false);
  };

  // Make the button accessible when is disabled change to false
  useEffect(() => {
    setDisabled(!selectedFolderId);
  }, [selectedFolderId]);

  return createPortal(
    <Modal
      isOpen={isOpen}
      onModalClose={onModalClose}
      id={'add-attachment-to-workspace-modal'}
      size="md"
    >
      <Modal.Header onModalClose={onModalClose}>
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
          onClick={onModalClose}
        >
          {t('cancel')}
        </Button>
        <Button
          type="submit"
          color="primary"
          variant="filled"
          onClick={handleAddAttachmentToWorkspace}
          disabled={isLoading || disabled}
          isLoading={isLoading}
        >
          {t('add')}
        </Button>
      </Modal.Footer>
    </Modal>,
    document.getElementById('portal') as HTMLElement,
  );
}
