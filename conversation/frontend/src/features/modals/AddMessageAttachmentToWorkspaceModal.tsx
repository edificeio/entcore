import { AddAttachmentToWorkspaceModal } from '@edifice.io/react';
import { useAppActions } from '~/store';

export function AddMessageAttachmentToWorkspaceModal() {
  const { setOpenFolderModal } = useAppActions();

  const handleCloseModal = () => setOpenFolderModal(null);

  return (
    <AddAttachmentToWorkspaceModal
      isOpen
      onCancel={handleCloseModal}
      onSuccess={handleCloseModal}
    />
  );
}
