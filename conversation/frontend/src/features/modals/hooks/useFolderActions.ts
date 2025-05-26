import { useAppActions, useSelectedFolders } from '~/store';
import { useToast } from '@edifice.io/react';
import {
  searchFolder,
  useCreateFolder,
  useFoldersTree,
  useTrashFolder,
  useRenameFolder,
} from '~/services';
import { useCallback, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useSelectedFolder } from '~/hooks/useSelectedFolder';
import { useI18n } from '~/hooks/useI18n';

const MAX_LENGTH = 50;

export function useFolderActions() {
  const { t } = useI18n();
  const { success, error } = useToast();
  const navigate = useNavigate();
  const currentFolder = useSelectedFolder();
  const foldersTreeQuery = useFoldersTree();
  const selectedFolders = useSelectedFolders();
  const { setSelectedFolders } = useAppActions();
  const createMutation = useCreateFolder();
  const trashMutation = useTrashFolder();
  const renameMutation = useRenameFolder();

  const [isPending, setIsPending] = useState<boolean | undefined>(undefined);

  const createFolder = useCallback(
    (name?: string, parentId?: string) => {
      name = name?.trim() ?? t('new.folder');
      if (name.length > MAX_LENGTH) {
        name = name.substring(0, MAX_LENGTH);
      }
      if (name.length === 0) {
        error(t('conversation.error.new.folder'));
        return false;
      }

      const foldersTree = foldersTreeQuery.data;
      if (!foldersTree) return;

      // Check if a folder with the same name exists at this level.
      const siblings = parentId
        ? foldersTree.find((f) => f.id === parentId)?.subFolders
        : foldersTree;
      if (siblings && siblings.findIndex((f) => f.name == name) !== -1) {
        throw new Error(t('conversation.error.duplicate.folder'));
      }

      // Create the folder
      createMutation.mutate(
        { name, parentId },
        {
          onSuccess: () => success(t('conversation.success.new.folder')),
          onError: () => error(t('conversation.error.new.folder')),
          onSettled: () => setIsPending(false),
        },
      );
      setIsPending(true);
    },
    [createMutation, error, foldersTreeQuery.data, success, t],
  );

  const trashFolder = useCallback(() => {
    if (selectedFolders.length > 0) {
      const id = selectedFolders[0].id;
      trashMutation.mutate(
        { id },
        {
          onSuccess: () => {
            success(t('conversation.success.trash.folder'));
            setSelectedFolders([]);
            if (
              id === currentFolder.folderId ||
              id === currentFolder.userFolder?.id
            ) {
              navigate('/inbox');
            }
          },
          onError: () => error(t('conversation.error.trash.folder')),
          onSettled: () => setIsPending(false),
        },
      );
      setIsPending(true);
    } else {
      return false;
    }
  }, [
    currentFolder.folderId,
    currentFolder.userFolder?.id,
    error,
    navigate,
    selectedFolders,
    setSelectedFolders,
    success,
    t,
    trashMutation,
  ]);

  const renameFolder = useCallback(
    (name: string) => {
      name = name.trim();
      if (name.length > MAX_LENGTH) {
        name = name.substring(0, MAX_LENGTH);
      }
      if (name.length === 0) {
        error(t('conversation.error.rename.folder'));
        return false;
      }

      if (selectedFolders.length === 0) return false;
      const id = selectedFolders[0].id;

      const foldersTree = foldersTreeQuery.data;
      if (!foldersTree) return false;

      const found = searchFolder(id, foldersTree);

      // Check if a folder with this name already exists at this level.
      const siblings = found?.parent
        ? (found.parent.subFolders ?? [])
        : foldersTree;
      if (siblings && siblings.findIndex((f) => f.name == name) !== -1) {
        error(t('conversation.error.duplicate.folder'));
        return false;
      }

      // Rename the folder
      renameMutation.mutate(
        { id, name },
        {
          onSuccess: () => success(t('conversation.success.rename.folder')),
          onError: () => error(t('conversation.error.rename.folder')),
          onSettled: () => setIsPending(false),
        },
      );
      setIsPending(true);
    },
    [selectedFolders, foldersTreeQuery.data, renameMutation, error, t, success],
  );

  return {
    foldersTree: foldersTreeQuery.data,
    createFolder,
    trashFolder,
    renameFolder,
    isActionPending: isPending,
  };
}
