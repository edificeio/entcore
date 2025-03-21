import { useNavigate } from 'react-router-dom';
import { Message } from '~/models';
import { baseUrl, useCreateOrUpdateDraft } from '~/services';
import { useAttachFiles, useRemoveFile } from '~/services/queries/attachment';

export function useMessageAttachments({ id }: Message) {
  const attachFileMutation = useAttachFiles();
  const detachFileMutation = useRemoveFile();

  // These hooks are required when attaching files to a new draft without any id.
  const createOrUpdateDraft = useCreateOrUpdateDraft();
  const navigate = useNavigate();

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
    attachFileMutation.mutateAsync(mutateVars, {
      onSuccess() {
        if (id) navigate(`/draft/message/${id}`);
      },
    });
  }

  async function detachFile(attachmentId: string) {
    return detachFileMutation.mutateAsync({
      draftId: id,
      attachmentId,
    });
  }

  return {
    downloadAllUrl,
    attachFiles,
    detachFile,
  };
}
