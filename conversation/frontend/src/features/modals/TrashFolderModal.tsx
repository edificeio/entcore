import { useAppActions } from '~/store';
import { ConfirmModal } from '@edifice.io/react';
import { useEffect } from 'react';
import { useI18n } from '~/hooks';
import { useFolderActions } from './hooks';

export function TrashFolderModal() {
  const { t } = useI18n();
  const { setOpenedModal } = useAppActions();
  const { trashFolder: handleTrashClick, isActionPending } = useFolderActions();

  useEffect(() => {
    if (isActionPending === false) setOpenedModal(undefined);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [isActionPending]);

  const handleCancelClick = () => setOpenedModal(undefined);

  return (
    <ConfirmModal
      id="modalFolderTrash"
      variant="ok/cancel"
      header={t('folder.trash.title')}
      body={t('folder.trash.body')}
      okText={t('delete')}
      onCancel={handleCancelClick}
      onSuccess={handleTrashClick}
      isOpen={true}
    ></ConfirmModal>
  );
}
