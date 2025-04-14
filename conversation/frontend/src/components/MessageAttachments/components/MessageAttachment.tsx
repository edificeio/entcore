import { Attachment, IconButton } from '@edifice.io/react';
import {
  IconDelete,
  IconDownload,
  IconFolderAdd,
} from '@edifice.io/react/icons';
import { useState } from 'react';
import { AddMessageAttachmentToWorkspaceModal } from '~/components/MessageAttachments/components/AddMessageAttachmentToWorkspaceModal';
import { useI18n } from '~/hooks';
import { useMessageAttachments } from '~/hooks/useMessageAttachments';
import { Attachment as AttachmentMetaData, Message } from '~/models';
import { useMessageUpdated } from '~/store';

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
  const [showAddToWorkspaceModal, setShowAddToWorkspaceModal] = useState(false);
  const messageUpdated = useMessageUpdated();
  const { detachFile, getDownloadUrl } = useMessageAttachments(
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
              onClick={() => setShowAddToWorkspaceModal(true)}
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
      {showAddToWorkspaceModal && (
        <AddMessageAttachmentToWorkspaceModal
          message={message}
          isOpen={showAddToWorkspaceModal}
          onModalClose={() => setShowAddToWorkspaceModal(false)}
          attachmentId={attachment.id}
        />
      )}
    </>
  );
}
