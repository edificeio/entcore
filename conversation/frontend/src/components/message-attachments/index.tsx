import { Button, Grid, IconButton } from '@edifice.io/react';
import { IconDownload, IconFolderAdd, IconPlus } from '@edifice.io/react/icons';
import clsx from 'clsx';
import { useI18n } from '~/hooks';
import { Attachment as AttachmentMetaData } from '~/models';
import { MessageAttachment } from './MessageAttachment';
import './index.css';
import { baseUrl } from '~/services';

export interface MessageAttachmentsProps {
  attachments: AttachmentMetaData[];
  messageId: string;
  editMode?: boolean;
}

export function MessageAttachments({
  attachments,
  messageId,
  editMode = false,
}: MessageAttachmentsProps) {
  const { common_t, t } = useI18n();
  const downloadUrl = `${baseUrl}/message/${messageId}/allAttachments`;

  const className = clsx(
    'bg-gray-300 rounded-2 px-12 py-8 message-attachments gap-8 d-flex flex-column',
    editMode && 'border message-attachments-edit mx-16',
  );

  if (!attachments.length && !editMode) return null;

  return (
    <div className={className} data-drag-handle>
      {!!attachments.length && (
        <>
          <div className="d-flex align-items-center justify-content-between border-bottom">
            <span className="caption fw-bold my-8">
              {common_t('attachments')}
            </span>
            {attachments.length > 1 && (
              <div>
                <IconButton
                  title={common_t('conversation.copy.all.toworkspace')}
                  color="tertiary"
                  type="button"
                  icon={<IconFolderAdd />}
                  variant="ghost"
                />
                <a href={downloadUrl} download>
                  <IconButton
                    title={common_t('download.all.attachment')}
                    color="tertiary"
                    type="button"
                    icon={<IconDownload />}
                    variant="ghost"
                  />
                </a>
              </div>
            )}
          </div>
          <ul className="d-flex gap-8 flex-wrap list-unstyled m-0 ">
            {attachments.map((attachment) => (
              <li>
                <MessageAttachment
                  attachment={attachment}
                  messageId={messageId}
                />
              </li>
            ))}
          </ul>
        </>
      )}
      {editMode && (
        <Button color="secondary" variant="ghost" leftIcon={<IconPlus />}>
          {t('add.attachment')}
        </Button>
      )}
    </div>
  );
}
