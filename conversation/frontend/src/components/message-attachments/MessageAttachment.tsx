import { Attachment, IconButton } from '@edifice.io/react';
import {
  IconDelete,
  IconDownload,
  IconFolderAdd,
} from '@edifice.io/react/icons';
import { useFolderHandlers } from '~/features/menu/hooks/useFolderHandlers';
import { useI18n } from '~/hooks';
import { useMessageAttachments } from '~/hooks/useMessageAttachments';
import { Attachment as AttachmentMetaData, Message } from '~/models';
import { baseUrl } from '~/services';

export interface MessageAttachmentsProps {
  attachment: AttachmentMetaData;
  message: Message;
  editMode?: boolean;
}

export function MessageAttachment({
  attachment,
  message,
  editMode,
}: MessageAttachmentsProps) {
  const { t } = useI18n();
  const { handleAddAttachmentToWorkspace } = useFolderHandlers();
  const { detachFile } = useMessageAttachments(message);

  const downloadUrl = `${baseUrl}/message/${message.id}/attachment/${attachment.id}`;

  const handleAddToWorkspace = () => {
    handleAddAttachmentToWorkspace();
  };

  return (
    <Attachment
      name={attachment.filename}
      options={
        <>
          <IconButton
            title={t('conversation.copy.toworkspace')}
            color="tertiary"
            type="button"
            icon={<IconFolderAdd />}
            variant="ghost"
            onClick={handleAddToWorkspace}
          />
          <a href={downloadUrl} download>
            <IconButton
              title={t('download.attachment')}
              color="tertiary"
              type="button"
              icon={<IconDownload />}
              variant="ghost"
            />
          </a>
          {editMode && (
            <IconButton
              title={t('remove.attachment')}
              color="danger"
              type="button"
              icon={<IconDelete />}
              variant="ghost"
              onClick={() => detachFile(attachment.id)}
            />
          )}
        </>
      }
    />
  );
}
