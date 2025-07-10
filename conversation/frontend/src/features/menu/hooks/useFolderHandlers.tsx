import { Folder } from '~/models';
import { useActionsStore } from '~/store/actions';

/**
 * Custom hook that provides handlers for folder-related actions.
 *
 * @returns {Object} An object containing the following handlers:
 * - `handleCreate`: Opens the folder creation modal.
 * - `handleMove`: Opens the folder move modal and sets the selected folder.
 * - `handleRename`: Opens the folder rename modal and sets the selected folder.
 * - `handleTrash`: Opens the folder trash modal and sets the selected folder.
 */
export const useFolderHandlers = () => {
  const setOpenedModal = useActionsStore.use.setOpenedModal();
  const setSelectedFolders = useActionsStore.use.setSelectedFolders();

  const handleCreate = () => {
    setOpenedModal('create');
  };

  const handleMove = (folder: Folder) => {
    setSelectedFolders([folder]);
    setOpenedModal('move');
  };

  const handleRename = (folder: Folder) => {
    setSelectedFolders([folder]);
    setOpenedModal('rename');
  };

  const handleTrash = (folder: Folder) => {
    setSelectedFolders([folder]);
    setOpenedModal('trash');
  };

  const handleMoveMessage = () => {
    setOpenedModal('move-message');
  };

  return {
    /** Opens the folder creation modal. */
    handleCreate,
    /** Opens the folder move modal and sets the selected folder. */
    handleMove,
    /** Opens the folder rename modal and sets the selected folder. */
    handleRename,
    /** Opens the folder trash modal and sets the selected folder. */
    handleTrash,
    /** Opens the message to move modal and sets the selected folder. */
    handleMoveMessage,
  };
};
