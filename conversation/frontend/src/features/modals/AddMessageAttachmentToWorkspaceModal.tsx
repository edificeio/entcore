import { AddAttachmentToWorkspaceModal } from '@edifice.io/react';
import { useAppActions } from '~/store';

export function AddMessageAttachmentToWorkspaceModal() {
  const { setOpenedModal } = useAppActions();

  const handleCloseModal = () => setOpenedModal(undefined);

  return (
    <AddAttachmentToWorkspaceModal
      isOpen
      onCancel={handleCloseModal}
      onSuccess={handleCloseModal}
    />
  );
}
