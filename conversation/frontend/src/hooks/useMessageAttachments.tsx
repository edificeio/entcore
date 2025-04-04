import { Attachment, Message } from '~/models';
import { baseUrl, useCreateOrUpdateDraft } from '~/services';
import { useAttachFiles, useDetachFile } from '~/services/queries/attachment';

export function useMessageAttachments({ id, attachments }: Message) {
  const attachFileMutation = useAttachFiles();
  const detachFileMutation = useDetachFile();

  // These hooks is required when attaching files to a blank new draft, without id.
  const createOrUpdateDraft = useCreateOrUpdateDraft();

  const downloadAllUrl = `${baseUrl}/message/${id}/allAttachments`;

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

  function copyToWorkspace() {
    //download attachment
    //upload to workspace
  }

  return {
    attachFiles,
    attachments,
    copyToWorkspace,
    detachFile,
    detachFiles,
    downloadAllUrl,
    isMutating: attachFileMutation.isPending || detachFileMutation.isPending,
  };
}
