import { MessageAttachmentsProps } from '~/components/message-attachments';
import { baseUrl } from '~/services';
import { useAttachFiles } from '~/services/queries/attachment';

export function useMessageAttachments({ messageId }: MessageAttachmentsProps) {
  const attachFileMutation = useAttachFiles();

  const downloadAllUrl = `${baseUrl}/message/${messageId}/allAttachments`;

  const attachFiles = (files: FileList | null) => {
    const mutateVars: { draftId: string; files: File[] } = {
      draftId: messageId,
      files: [],
    };
    for (let i = 0; files && i < files.length; i++) {
      const file = files.item(i);
      if (file) mutateVars.files.push(file);
    }

    attachFileMutation.mutate(mutateVars);
  };

  return {
    downloadAllUrl,
    attachFiles,
  };
}
