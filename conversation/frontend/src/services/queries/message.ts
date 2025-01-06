import {
  queryOptions,
  useMutation,
  useQuery,
  useQueryClient,
} from '@tanstack/react-query';
import { folderQueryOptions, messageService } from '..';

/**
 * Message Query Options Factory.
 */
export const messageQueryOptions = {
  base: ['message'] as const,
  getById(messageId: string) {
    return queryOptions({
      queryKey: [...messageQueryOptions.base, messageId] as const,
      queryFn: () => messageService.getById(messageId),
      staleTime: 5000,
    });
  },
};

/*
 * All queries and mutations
 */
export const useMessage = (messageId: string) => {
  return useQuery(messageQueryOptions.getById(messageId));
};

export const useMarkRead = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id }: { id: string | string[] }) =>
      messageService.markRead(id),
    onSuccess: (_data, { id }) => {
      const messageIds = typeof id === 'string' ? [id] : id;
      messageIds.forEach((messageId) => {
        queryClient.invalidateQueries({
          queryKey: messageQueryOptions.getById(messageId).queryKey,
        });
      });
    },
  });
};

export const useMarkUnread = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id }: { id: string | string[] }) =>
      messageService.markUnread(id),
    onSuccess: (_data, { id }) => {
      const messageIds = typeof id === 'string' ? [id] : id;
      messageIds.forEach((messageId) => {
        queryClient.invalidateQueries({
          queryKey: messageQueryOptions.getById(messageId).queryKey,
        });
      });
    },
  });
};

export const useTrashMessage = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id }: { id: string | string[] }) =>
      messageService.moveToFolder('trash', id),
    onSuccess: (_data, { id }) => {
      const messageIds = typeof id === 'string' ? [id] : id;
      messageIds.forEach((messageId) => {
        queryClient.invalidateQueries({
          queryKey: messageQueryOptions.getById(messageId).queryKey,
        });
      });
    },
  });
};

export const useRestoreMessage = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id }: { id: string | string[] }) =>
      messageService.restore(id),
    onSuccess: (_data, { id }) => {
      const messageIds = typeof id === 'string' ? [id] : id;
      messageIds.forEach((messageId) => {
        queryClient.invalidateQueries({
          queryKey: messageQueryOptions.getById(messageId).queryKey,
        });
      });
    },
  });
};

export const useDeleteMessage = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id }: { id: string | string[] }) =>
      messageService.delete(id),
    onSuccess: (_data, { id }) => {
      const messageIds = typeof id === 'string' ? [id] : id;
      messageIds.forEach((messageId) => {
        queryClient.invalidateQueries({
          queryKey: messageQueryOptions.getById(messageId).queryKey,
        });
      });
    },
  });
};

export const useMoveMessage = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({
      folderId,
      id,
    }: {
      folderId: string;
      id: string | string[];
    }) => messageService.moveToFolder(folderId, id),
    onSuccess: (_data, { id }) => {
      const messageIds = typeof id === 'string' ? [id] : id;
      messageIds.forEach((messageId) => {
        queryClient.invalidateQueries({
          queryKey: messageQueryOptions.getById(messageId).queryKey,
        });
      });
    },
  });
};

export const useCreateDraft = () => {
  return useMutation({
    mutationFn: ({
      payload,
    }: {
      payload: {
        subject?: string;
        body?: string;
        to?: string[];
        cc?: string[];
        cci?: string[];
      };
    }) => messageService.createDraft(payload),
  });
};

export const useUpdateDraft = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({
      draftId,
      payload,
    }: {
      draftId: string;
      payload: {
        subject?: string;
        body?: string;
        to?: string[];
        cc?: string[];
        cci?: string[];
      };
    }) => messageService.updateDraft(draftId, payload),
    onSuccess: (_data, { draftId }) => {
      queryClient.invalidateQueries({
        queryKey: messageQueryOptions.getById(draftId).queryKey,
      });
    },
  });
};

export const useSendDraft = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({
      draftId,
      payload,
    }: {
      draftId: string;
      payload?: {
        subject?: string;
        body?: string;
        to?: string[];
        cc?: string[];
        cci?: string[];
      };
    }) => messageService.send(draftId, payload),
    onSuccess: (_data /*, { draftId }*/) => {
      // TODO optimistic update ?
      queryClient.invalidateQueries({
        queryKey: folderQueryOptions.getFoldersTree().queryKey,
      });
    },
  });
};
