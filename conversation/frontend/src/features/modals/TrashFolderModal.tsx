import { useAppActions } from '~/store';
import { Button, Modal } from '@edifice.io/react';
import { useEffect } from 'react';
import { useI18n } from '~/hooks';
import { useFolderActions } from './hooks';

export function TrashFolderModal() {
  const { t, common_t } = useI18n();
  const { setOpenedModal } = useAppActions();
  const { trashFolder: handleTrashClick, isActionPending } = useFolderActions();

  useEffect(() => {
    if (isActionPending === false) setOpenedModal(undefined);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [isActionPending]);

  const handleClose = () => setOpenedModal(undefined);

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
