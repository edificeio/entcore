import { Folder } from '~/models';
import { useAppActions } from '~/store';

/**
 * Custom hook that provides handlers for folder-related actions.
 *
 * @returns {Object} An object containing the following handlers:
 * - `handleCreate`: Opens the folder creation modal.
 * - `handleMove`: Opens the folder move modal and sets the selected folder.
 * - `handleRename`: Opens the folder rename modal and sets the selected folder.
 * - `handleTrash`: Opens the folder trash modal and sets the selected folder.
 */
export function useFolderHandlers() {
  const { setOpenFolderModal, setSelectedFolders } = useAppActions();

  const handleCreate = () => {
    setOpenFolderModal('create');
  };

  const handleMove = (folder: Folder) => {
    setSelectedFolders([folder]);
    setOpenFolderModal('move');
  };

  const handleRename = (folder: Folder) => {
    setSelectedFolders([folder]);
    setOpenFolderModal('rename');
  };

  const handleTrash = (folder: Folder) => {
    setSelectedFolders([folder]);
    setOpenFolderModal('trash');
  };

  const handleMoveMessage = () => {
    setOpenFolderModal('move-message');
  };

  const handleAddAttachmentToWorkspace = () => {
    setOpenFolderModal('add-attachment-to-workspace');
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
    /** Opens the modal to add an message attachment to a the selected folder in doc space. */
    handleAddAttachmentToWorkspace,
  };
}
