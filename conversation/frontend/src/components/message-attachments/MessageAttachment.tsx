import { Attachment, IconButton } from '@edifice.io/react';
import { IconDownload, IconFolderAdd } from '@edifice.io/react/icons';
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
  const { common_t } = useI18n();

  const downloadUrl = `${baseUrl}/message/${messageId}/attachement/${attachment.id}`;

  return (
    <Attachment
      name={attachment.filename}
      options={
        <>
          <IconButton
            title={common_t('conversation.copy.toworkspace')}
            color="tertiary"
            type="button"
            icon={<IconFolderAdd />}
            variant="ghost"
          />
          <a href={downloadUrl} download>
            <IconButton
              title={common_t('download.attachment')}
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
