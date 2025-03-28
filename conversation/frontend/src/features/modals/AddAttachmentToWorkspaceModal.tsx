import { Button, Modal } from '@edifice.io/react';
import { useEffect, useRef } from 'react';
import { useI18n } from '~/hooks';
import { useAppActions } from '~/store';

export function AddAttachmentToWorkspaceModal() {
  const { t, common_t } = useI18n();
  const { setOpenFolderModal } = useAppActions();
  const refInputName = useRef<HTMLInputElement>(null);

  useEffect(() => {
    refInputName.current?.focus();
  }, []);

  const handleCloseModal = () => setOpenFolderModal(null);

  return (
    <Modal
      size="md"
      id="modalMovaAttachmentToFolder"
      isOpen={true}
      onModalClose={handleCloseModal}
    >
      <Modal.Header onModalClose={handleCloseModal}>
        {t('attachments.add.to.folder')}
      </Modal.Header>

      <Modal.Body>
        <p>{t('attachments.add.to.folder.description')}</p>
      </Modal.Body>

      <Modal.Footer>
        <Button
          type="button"
          color="tertiary"
          variant="ghost"
          onClick={handleCloseModal}
        >
          {common_t('cancel')}
        </Button>
        <Button
          type="submit"
          color="primary"
          variant="filled"
          onClick={() => {}}
        >
          {t('move')}
        </Button>
      </Modal.Footer>
    </Modal>
  );
}
