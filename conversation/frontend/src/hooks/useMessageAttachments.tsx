import { useNavigate } from 'react-router-dom';
import { Message } from '~/models';
import { baseUrl, useCreateOrUpdateDraft } from '~/services';
import { useAttachFiles, useRemoveFile } from '~/services/queries/attachment';
import { useAppActions, useMessageUpdated } from '~/store';

export function useMessageAttachments({
  id,
  attachments,
  ...message
}: Message) {
  const attachFileMutation = useAttachFiles();
  const detachFileMutation = useRemoveFile();

  // These hooks are required when attaching files to a new draft without any id.
  const createOrUpdateDraft = useCreateOrUpdateDraft();
  const { setMessageUpdated } = useAppActions();
  const messageUpdated = useMessageUpdated();
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
      onSuccess(data, { files }) {
        if (id) {
          navigate(`/draft/message/${id}`);
        } else {
          if (messageUpdated) {
            //Optimistic update
            const { attachments } = messageUpdated;
            for (let j = 0; j < mutateVars.files.length; j++) {
              if (j < data.length && data[j]) {
                attachments.push({
                  id: data[j].id,
                  filename: mutateVars.files[j].name,
                  name: mutateVars.files[j].name,
                  size: mutateVars.files[j].size,
                  charset: '',
                  contentTransferEncoding: '',
                  contentType: '',
                });
              }
            }
            setMessageUpdated({ ...messageUpdated });
          }
        }
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
    attachments,
    downloadAllUrl,
    attachFiles,
    detachFile,
  };
}
