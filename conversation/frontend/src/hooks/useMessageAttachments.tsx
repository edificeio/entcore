import { useToast } from '@edifice.io/react';
import { odeServices } from 'edifice-ts-client';
import { Attachment, Message } from '~/models';
import { baseUrl, useCreateOrUpdateDraft } from '~/services';

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
    return detachFileMutation.mutateAsync({
      draftId: id,
      attachmentId,
    });
  }

  function detachFiles(attachments: Attachment[]) {
    return Promise.all(attachments.map(({ id }) => detachFile(id)));
  }

  async function copyToWorkspace(
    attachment: Attachment,
    selectedFolderId: string,
  ) {
    try {
      const attachmentBlob = await downloadAttachmentMutation.mutateAsync({
        messageId: id,
        attachmentId: attachment.id,
      });
      if (!attachmentBlob) return;

      const file = new File([attachmentBlob], attachment.filename, {
        type: attachment.contentType,
      });
      await odeServices.workspace().saveFile(file, {
        parentId: selectedFolderId,
      });
    } catch (error) {
      let errorMessage = t('conversation.error.copyToWorkspace');
      if (error) errorMessage += `: ${t(error as string)}`; //TODO type the error in the Ode services
      toast.error(errorMessage);
      return false;
    }
    toast.success(t('conversation.notify.copyToWorkspace'));

    return true;
  }

  return {
    attachFiles,
    attachments,
    copyToWorkspace,
    detachFile,
    detachFiles,
    downloadAllUrl,
    getDownloadUrl,
    isMutating: attachFileMutation.isPending || detachFileMutation.isPending,
  };
}
