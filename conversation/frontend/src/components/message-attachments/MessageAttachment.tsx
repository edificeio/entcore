import { Attachment, IconButton } from '@edifice.io/react';
import { IconDownload, IconFolderAdd } from '@edifice.io/react/icons';
import { useFolderHandlers } from '~/features/menu/hooks/useFolderHandlers';
import { useI18n } from '~/hooks';
import { Attachment as AttachmentMetaData } from '~/models';
import { baseUrl } from '~/services';

export interface MessageAttachmentsProps {
  attachment: AttachmentMetaData;
  messageId: string;
}

export function MessageAttachment({
  attachment,
  messageId,
}: MessageAttachmentsProps) {
  const { t } = useI18n();
  const { handleMoveAttachment } = useFolderHandlers();

  const downloadUrl = `${baseUrl}/message/${messageId}/attachment/${attachment.id}`;

  const handleAddToWorkspace = () => {
    handleMoveAttachment();
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
        </>
      }
    />
  );
}
