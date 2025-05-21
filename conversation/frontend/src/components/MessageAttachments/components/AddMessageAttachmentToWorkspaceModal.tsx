import { Button, Modal } from '@edifice.io/react';
import { WorkspaceFolders } from '@edifice.io/react/multimedia';
import { useEffect, useState } from 'react';
import { createPortal } from 'react-dom';
import { useI18n } from '~/hooks';
import { useMessageAttachments } from '~/hooks/useMessageAttachments';
import { Attachment } from '~/models';

interface AddMessageAttachmentToWorkspaceModalProps {
  attachments: Attachment[];
  onModalClose: () => void;
  isOpen?: boolean;
}
export function AddMessageAttachmentToWorkspaceModal({
  attachments,
  isOpen = false,
  onModalClose,
}: AddMessageAttachmentToWorkspaceModalProps) {
  const { t } = useI18n();
  const { copyToWorkspace } = useMessageAttachments();
  const [selectedFolderIdToCopyFile, setSelectedFolderIdToCopyFile] = useState<
    string | undefined
  >(undefined);
  const [isLoading, setIsLoading] = useState(false);
  const [disabled, setDisabled] = useState(false);

  const handleFolderSelected = (folderId: string, canCopyFileInto: boolean) => {
    setSelectedFolderIdToCopyFile(canCopyFileInto ? folderId : undefined);
  };

  const handleAddAttachmentToWorkspace = async () => {
    if (selectedFolderIdToCopyFile === undefined) return;
    setIsLoading(true);
    const isSuccess = await copyToWorkspace(
      attachments,
      selectedFolderIdToCopyFile,
    );
    if (isSuccess) {
      onModalClose();
    }
    setIsLoading(false);
  };

  // Make the button accessible when is disabled change to false
  useEffect(() => {
    setDisabled(selectedFolderIdToCopyFile === undefined);
  }, [selectedFolderIdToCopyFile]);

  return createPortal(
    <Modal
      isOpen={isOpen}
      onModalClose={onModalClose}
      id={'add-attachment-to-workspace-modal'}
      size="md"
    >
      <Modal.Header onModalClose={onModalClose}>
        {t('attachments.add.to.folder.modal.title')}
      </Modal.Header>
      <Modal.Body>
        <div className="d-flex flex-column gap-12">
          <p>
            {t('attachments.add.to.folder.modal.description', {
              count: attachments.length,
            })}
          </p>

          <WorkspaceFolders onFolderSelected={handleFolderSelected} />
        </div>
      </Modal.Body>
      <Modal.Footer>
        <Button
          type="button"
          color="tertiary"
          variant="ghost"
          onClick={onModalClose}
        >
          {t('attachments.add.to.folder.modal.cancel')}
        </Button>
        <Button
          type="submit"
          color="primary"
          variant="filled"
          onClick={handleAddAttachmentToWorkspace}
          disabled={isLoading || disabled}
          isLoading={isLoading}
        >
          {t('attachments.add.to.folder.modal.add')}
        </Button>
      </Modal.Footer>
    </Modal>,
    document.getElementById('portal') as HTMLElement,
  );
}
