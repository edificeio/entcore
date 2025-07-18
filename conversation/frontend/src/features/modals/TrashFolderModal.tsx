import { ConfirmModal } from '@edifice.io/react';
import { useEffect } from 'react';
import { useI18n } from '~/hooks/useI18n';
import { useActionsStore } from '~/store/actions';
import { useFolderActions } from './hooks';
import './FolderModalInDropdown.css';

export function TrashFolderModal() {
  const { t } = useI18n();
  const setOpenedModal = useActionsStore.use.setOpenedModal();
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
      size="sm"
      onCancel={handleCancelClick}
      onSuccess={handleTrashClick}
      isOpen={true}
    ></ConfirmModal>
  );
}
