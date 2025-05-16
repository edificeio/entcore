import { useToast } from '@edifice.io/react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useI18n } from '~/hooks';
import { Message } from '~/models';
import { useAppActions } from '~/store';
import { attachmentService, messageQueryOptions, useFolderUtils } from '..';

/**
 * Hook to attach many Files to a draft message.
 * @returns Mutation result for attaching the Files.
 */
export const useAttachFiles = () => {
  const queryClient = useQueryClient();
  const { setMessageUpdated } = useAppActions();
  const { updateFolderMessagesQueryData } = useFolderUtils();
  const toast = useToast();
  const { t } = useI18n();

  return useMutation({
    mutationFn: ({ draftId, files }: { draftId: string; files: File[] }) =>
      Promise.all(files.map((file) => attachmentService.attach(draftId, file))),
    async onSuccess(_ids, { draftId }) {
      await queryClient.invalidateQueries({
        queryKey: messageQueryOptions.getById(draftId).queryKey,
      });

      // Refresh the message data after attaching files to the draft message
      // This is necessary to update the message state in the store
      const message = await queryClient.ensureQueryData(
        messageQueryOptions.getById(draftId),
      );
      if (message) {
        setMessageUpdated({
          ...message,
        });
      }
      updateFolderMessagesQueryData('draft', (oldMessage) =>
        oldMessage.id === draftId
          ? { ...oldMessage, date: Date.now(), hasAttachment: true }
          : oldMessage,
      );
      toast.success(t('attachments.loaded'));
    },
    onError(_error, { draftId }) {
      queryClient.invalidateQueries({
        queryKey: messageQueryOptions.getById(draftId).queryKey,
      });
    },
  });
};

/**
 * Hook to remove an attachment from a draft message.
 * @returns Mutation result for removing the File.
 */
export const useDetachFile = () => {
  const queryClient = useQueryClient();
  const { setMessageUpdated } = useAppActions();
  const { updateFolderMessagesQueryData } = useFolderUtils();

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
            date: Date.now(),
          };
          setMessageUpdated(updatedMessage);
          return updatedMessage;
        },
      );
      updateFolderMessagesQueryData('draft', (oldMessage) =>
        oldMessage.id === draftId
          ? { ...oldMessage, date: Date.now() }
          : oldMessage,
      );
    },
    onError(_error, { draftId }) {
      queryClient.invalidateQueries({
        queryKey: messageQueryOptions.getById(draftId).queryKey,
      });
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
