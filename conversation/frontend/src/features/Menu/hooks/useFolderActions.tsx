import { Folder } from '~/models';
import { useAppActions } from '~/store';

export function useFolderActions() {
  const { setOpenFolderModal } = useAppActions();

  const handleCreate = () => {
    setOpenFolderModal('create');
  };

  const handleMove = (_folder: Folder) => {
    setOpenFolderModal('move');
  };

  const handleRename = (_folder: Folder) => {
    setOpenFolderModal('rename');
  };

  const handleDelete = (_folder: Folder) => {
    setOpenFolderModal('delete');
  };

  return {
    handleCreate,
    handleMove,
    handleRename,
    handleDelete,
  };
}
