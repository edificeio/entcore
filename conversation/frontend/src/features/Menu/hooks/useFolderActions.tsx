import { Folder } from '~/models';

export function useFolderActions({ folder }: { folder: Folder }) {
  const handleMove = () => {
    alert(`move ${folder.name}`);
  };

  const handleRename = () => {
    alert(`rename ${folder.name}`);
  };

  const handleDelete = () => {
    alert(`delete ${folder.name}`);
  };

  return {
    handleMove,
    handleRename,
    handleDelete,
  };
}
