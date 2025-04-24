import { useToast } from '@edifice.io/react';
import { odeServices } from 'edifice-ts-client';
import { Attachment, Message } from '~/models';
import { baseUrl, useCreateOrUpdateDraft } from '~/services';

import { useState } from 'react';
import {
  useAttachFiles,
  useDetachFile,
  useDownloadAttachment,
} from '~/services/queries/attachment';
import { useI18n } from './useI18n';

export function useMessageAttachments({ id, attachments }: Message) {
  const attachFileMutation = useAttachFiles();
  const detachFileMutation = useDetachFile();
  const downloadAttachmentMutation = useDownloadAttachment();
  const [detachInProgress, setDetachInProgress] = useState(new Set<string>());
  const toast = useToast();
  const { t } = useI18n();

  // These hooks is required when attaching files to a blank new draft, without id.
  const createOrUpdateDraft = useCreateOrUpdateDraft();

  const downloadAllUrl = `${baseUrl}/message/${id}/allAttachments`;

  const getDownloadUrl = (attachementId: string) =>
    `${baseUrl}/message/${id}/attachment/${attachementId}`;

  async function attachFiles(files: FileList | null) {
    if (!id) {
      // Save this new draft to get its id
      const promise = await createOrUpdateDraft();
      if (promise) id = promise.id;
    }
    const mutateVars: { draftId: string; files: File[] } = {
      draftId: id,
      files: [],
    };
    for (let i = 0; files && i < files.length; i++) {
      const file = files.item(i);
      if (file) mutateVars.files.push(file);
    }
    attachFileMutation.mutateAsync(mutateVars);
  }

  function detachFile(attachmentId: string) {
    setDetachInProgress((prev) => new Set(prev).add(attachmentId));
    return detachFileMutation.mutateAsync(
      {
        draftId: id,
        attachmentId,
      },
      {
        onSuccess: () => {
          attachments = attachments.filter(
            (attachment) => attachment.id !== attachmentId,
          );
        },
        onError: () => {
          setDetachInProgress((prev) => {
            const newSet = new Set(prev);
            newSet.delete(attachmentId);
            return newSet;
          });
        },
      },
    );
  }

  function detachFiles(attachments: Attachment[]) {
    return Promise.all(attachments.map(({ id }) => detachFile(id)));
  }

  async function copyToWorkspace(
    attachments: Attachment[],
    selectedFolderId: string,
  ) {
    try {
      const downloadFilesPromises = attachments.map(async (attachment) => {
        const attachmentBlob = await downloadAttachmentMutation.mutateAsync({
          messageId: id,
          attachmentId: attachment.id,
        });

        if (!attachmentBlob) return;

        return new File([attachmentBlob], attachment.filename, {
          type: attachment.contentType,
        });
      });

      const files = await Promise.all(downloadFilesPromises);

      // 2 - Send files to workspace
      const addFilesPromises = files.map((file) => {
        if (!file) return;

        return odeServices.workspace().saveFile(file, {
          parentId: selectedFolderId,
        });
      });
      await Promise.all(addFilesPromises);

      toast.success(
        t('conversation.notify.copyToWorkspace', { count: files.length }),
      );

      return true;
    } catch (error) {
      let errorMessage = t('conversation.error.copyToWorkspace');
      errorMessage += `: ${t(error as string)}`; //force the type beacause the error type should be defined as string in the Ode services in the frontend-framework
      toast.error(errorMessage);
      return false;
    }
  }

  return {
    attachFiles,
    attachments,
    copyToWorkspace,
    detachFile,
    detachFiles,
    detachInProgress,
    downloadAllUrl,
    getDownloadUrl,
    isMutating: attachFileMutation.isPending || detachFileMutation.isPending,
  };
}
