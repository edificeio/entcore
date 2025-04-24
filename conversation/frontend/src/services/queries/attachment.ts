import { useToast } from '@edifice.io/react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useI18n } from '~/hooks';
import { Message } from '~/models';
import { useAppActions } from '~/store';
import {
  attachmentService,
  messageQueryOptions,
  useCreateOrUpdateDraft,
} from '..';

/**
 * Hook to attach many Files to a draft message.
 * @returns Mutation result for attaching the Files.
 */
export const useAttachFiles = () => {
  const queryClient = useQueryClient();
  const updateDraft = useCreateOrUpdateDraft();
  const { setMessageUpdated } = useAppActions();

  const toast = useToast();
  const { t } = useI18n();

  return useMutation({
    mutationFn: ({ draftId, files }: { draftId: string; files: File[] }) =>
      Promise.all(files.map((file) => attachmentService.attach(draftId, file))),
    onSuccess(_ids, { draftId }) {
      updateDraft()?.then(() => {
        queryClient
          .invalidateQueries({
            queryKey: messageQueryOptions.getById(draftId).queryKey,
          })
          .then(() => {
            // Refresh the message data after attaching files to the draft message
            // This is necessary to update the message state in the store
            queryClient
              .ensureQueryData(messageQueryOptions.getById(draftId))
              .then((message) => {
                if (message) {
                  setMessageUpdated({
                    ...message,
                  });
                }
              });
          });
        toast.success(t('attachments.loaded'));
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

/**
 * Hook to remove an attachment from a draft message.
 * @returns Mutation result for removing the File.
 */
export const useDetachFile = () => {
  const queryClient = useQueryClient();
  const toast = useToast();
  const { setMessageUpdated } = useAppActions();

  return useMutation({
    mutationFn: ({
      draftId,
      attachmentId,
    }: {
      draftId: string;
      attachmentId: string;
    }) => attachmentService.detach(draftId, attachmentId),
    onSuccess(_ids, { draftId, attachmentId }) {
      queryClient.setQueryData(
        messageQueryOptions.getById(draftId).queryKey,
        (oldData: Message | undefined) => {
          if (!oldData) return undefined;
          const updatedMessage = {
            ...oldData,
            attachments: oldData.attachments.filter(
              (attachment) => attachment.id !== attachmentId,
            ),
          };
          setMessageUpdated(updatedMessage);
          return updatedMessage;
        },
      );
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
