import { Attachment, Message } from '~/models';
import { baseUrl, useCreateOrUpdateDraft } from '~/services';
import {
  useAttachFiles,
  useDetachFile,
  useDownloadAttachment,
} from '~/services/queries/attachment';

export function useMessageAttachments({ id, attachments }: Message) {
  const attachFileMutation = useAttachFiles();
  const detachFileMutation = useDetachFile();
  const downloadAttachmentMutation = useDownloadAttachment();

  // These hooks is required when attaching files to a blank new draft, without id.
  const createOrUpdateDraft = useCreateOrUpdateDraft();

  const downloadAllUrl = `${baseUrl}/message/${id}/allAttachments`;

  const getDownloadUrl = (attachementId: string) =>
    `${baseUrl}/message/${id}/attachment/${attachementId}`;

  async function attachFiles(files: FileList | null) {
    if (!id) {
      // Save this new draft to get its id
      const promise = createOrUpdateDraft();
      if (promise) id = (await promise).id;
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
    attachementId: string,
    selectedFolderId: string,
  ) {
    const attachmentBlob = await downloadAttachmentMutation.mutateAsync({
      messageId: id,
      attachmentId: attachementId,
    });

    console.log('attachmentBlob:', attachmentBlob);
    console.log('selectedFolderId:', selectedFolderId);
    console.log('attachementId:', attachementId);
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
