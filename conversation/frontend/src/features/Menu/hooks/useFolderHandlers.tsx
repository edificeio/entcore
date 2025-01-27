import { Folder } from '~/models';
import { useAppActions } from '~/store';

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

  return {
    handleCreate,
    handleMove,
    handleRename,
    handleTrash,
  };
}
