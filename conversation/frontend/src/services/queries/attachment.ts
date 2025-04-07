import { useToast } from '@edifice.io/react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { attachmentService, messageQueryOptions } from '..';
import { useI18n } from '~/hooks';

/**
 * Hook to attach many Files to a draft message.
 * @returns Mutation result for attaching the Files.
 */
export const useAttachFiles = () => {
  const queryClient = useQueryClient();
  const toast = useToast();
  const { t } = useI18n();

  return useMutation({
    mutationFn: ({ draftId, files }: { draftId: string; files: File[] }) =>
      Promise.all(files.map((file) => attachmentService.attach(draftId, file))),
    onSuccess(_ids, { draftId }) {
      queryClient.invalidateQueries({
        queryKey: messageQueryOptions.getById(draftId).queryKey,
      });
      toast.success(t('attachments.loaded'));
    },
    onError(error, { draftId }) {
      queryClient.invalidateQueries({
        queryKey: messageQueryOptions.getById(draftId).queryKey,
      });
      toast.error(error.message);
    },
  });
};

/**
 * Hook to remove an attachment from a draft message.
 * @returns Mutation result for removing the File.
 */
export const useDetachFile = () => {
  const queryClient = useQueryClient();
  const toast = useToast();

  return useMutation({
    mutationFn: ({
      draftId,
      attachmentId,
    }: {
      draftId: string;
      attachmentId: string;
    }) => attachmentService.detach(draftId, attachmentId),
    onSuccess(_ids, { draftId }) {
      queryClient.invalidateQueries({
        queryKey: messageQueryOptions.getById(draftId).queryKey,
      });
    },
    onError(error, { draftId }) {
      queryClient.invalidateQueries({
        queryKey: messageQueryOptions.getById(draftId).queryKey,
      });
      toast.error(error.message);
    },
  });
};

export const useDownloadAttachment = () => {
  return useMutation({
    mutationFn: ({
      messageId,
      attachmentId,
    }: {
      messageId: string;
      attachmentId: string;
    }) => attachmentService.downloadBlob(messageId, attachmentId),
  });
};
