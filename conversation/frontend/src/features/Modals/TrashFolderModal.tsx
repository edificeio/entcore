import { useAppActions } from '~/store';
import { Button, Modal } from '@edifice.io/react';
import { useEffect } from 'react';
import { useFolderActions, useI18n } from '~/hooks';

export function TrashFolderModal() {
  const { t, common_t } = useI18n();
  const { setOpenFolderModal } = useAppActions();
  const { trashFolder: handleTrashClick, isActionPending } = useFolderActions();

  useEffect(() => {
    if (isActionPending === false) setOpenFolderModal(null);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [isActionPending]);

  const handleClose = () => setOpenFolderModal(null);

  return (
    <Modal
      size="sm"
      id="modalFolderTrash"
      isOpen={true}
      onModalClose={handleClose}
    >
      <Modal.Header onModalClose={handleClose}>{t('put.trash')}</Modal.Header>

      <Modal.Body>{t('folder.trash.body')}</Modal.Body>

      <Modal.Footer>
        <Button
          type="button"
          color="tertiary"
          variant="ghost"
          onClick={handleClose}
        >
          {common_t('cancel')}
        </Button>
        <Button
          type="button"
          color="primary"
          variant="filled"
          onClick={handleTrashClick}
          isLoading={isActionPending === true}
          disabled={isActionPending === true}
        >
          {common_t('delete')}
        </Button>
      </Modal.Footer>
    </Modal>
  );
}
