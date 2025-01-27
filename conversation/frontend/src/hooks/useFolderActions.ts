import { useFoldersTree } from '~/store';
import { useI18n } from './useI18n';
import { useToast } from '@edifice.io/react';
import { useCreateFolder } from '~/services';
import { useCallback, useState } from 'react';

const MAX_LENGTH = 50;

export function useFolderActions() {
  const { t } = useI18n();
  const { success, error } = useToast();
  const foldersTree = useFoldersTree();
  const createMutation = useCreateFolder();

  const [isPending, setIsPending] = useState<boolean | undefined>(undefined);

  const createFolder = useCallback(
    (name?: string, parentId?: string) => {
      name = name?.trim() ?? t('new.folder');
      if (name.length > MAX_LENGTH) {
        name = name.substring(0, MAX_LENGTH);
      }

      // Check if a folder with the same name exists at this level.
      const siblings = parentId
        ? foldersTree.find((f) => f.id === parentId)?.subFolders
        : foldersTree;
      if (siblings && siblings.findIndex((f) => f.name == name) !== -1) {
        error(t('conversation.error.duplicate.folder'));
        return false;
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
    [createMutation, error, foldersTree, success, t],
  );

  return { foldersTree, createFolder, isActionPending: isPending };
}
