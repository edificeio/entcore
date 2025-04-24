import { Attachment, IconButton } from '@edifice.io/react';
import {
  IconDelete,
  IconDownload,
  IconFolderAdd,
} from '@edifice.io/react/icons';
import { useI18n } from '~/hooks';
import { useMessageAttachments } from '~/hooks/useMessageAttachments';
import { Attachment as AttachmentMetaData, Message } from '~/models';
import { useMessageUpdated } from '~/store';

export interface MessageAttachmentsProps {
  attachment: AttachmentMetaData;
  message: Message;
  onWantAddToWorkspace: (attachment: AttachmentMetaData) => void;
  editMode?: boolean;
}

export function MessageAttachment({
  attachment,
  message,
  onWantAddToWorkspace,
  editMode,
}: MessageAttachmentsProps) {
  const { t } = useI18n();
  const messageUpdated = useMessageUpdated();
  const { detachFile, detachInProgress, getDownloadUrl } =
    useMessageAttachments(
      editMode && messageUpdated ? messageUpdated : message,
    );

  const downloadUrl = getDownloadUrl(attachment.id);

  return (
    <>
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
                onClick={() => detachFile(attachment.id)}
                disabled={detachInProgress.has(attachment.id)}
              />
            )}
          </>
        }
      />
    </>
  );
}
