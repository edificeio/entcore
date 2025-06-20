import { Attachment, IconButton } from '@edifice.io/react';
import {
  IconDelete,
  IconDownload,
  IconFolderAdd,
} from '@edifice.io/react/icons';
import { useI18n } from '~/hooks/useI18n';
import { useMessageAttachments } from '~/hooks/useMessageAttachments';
import { Attachment as AttachmentMetaData } from '~/models';

export interface MessageAttachmentsProps {
  attachment: AttachmentMetaData;
  onWantAddToWorkspace: (attachment: AttachmentMetaData) => void;
  onDelete: (attachmentId: string) => void;
  editMode?: boolean;
}

export function MessageAttachment({
  attachment,
  onWantAddToWorkspace,
  onDelete,
  editMode,
}: MessageAttachmentsProps) {
  const { t } = useI18n();
  const { detachInProgress, getDownloadUrl } = useMessageAttachments();

  const downloadUrl = getDownloadUrl(attachment.id);

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
            onClick={() => onWantAddToWorkspace(attachment)}
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
              onClick={() => onDelete(attachment.id)}
              disabled={detachInProgress.has(attachment.id)}
            />
          )}
        </>
      }
    />
  );
}
