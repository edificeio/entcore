import { Button, Grid, IconButton } from '@edifice.io/react';
import { IconDownload, IconFolderAdd, IconPlus } from '@edifice.io/react/icons';
import clsx from 'clsx';
import { useI18n } from '~/hooks';
import { Attachment as AttachmentMetaData } from '~/models';
import { MessageAttachment } from './MessageAttachment';
import './index.css';

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

  const className = clsx(
    'mt-16 bg-gray-300 rounded-2 px-12 py-8 message-attachments ',
    editMode && 'border message-attachments-edit mx-16',
  );

  if (!attachments.length && !editMode) return null;

  return (
    <div className={className} data-drag-handle>
      {!!attachments.length && (
        <>
          <div className="d-flex align-items-center justify-content-between mb-8 mt-0 border-bottom">
            <span>{common_t('attachments')}</span>
            <div>
              <IconButton
                title={common_t('conversation.copy.all.toworkspace')}
                color="tertiary"
                type="button"
                icon={<IconFolderAdd />}
                variant="ghost"
              />
              <IconButton
                title={common_t('download.all.attachment')}
                color="tertiary"
                type="button"
                icon={<IconDownload />}
                variant="ghost"
              />
            </div>
          </div>
          <Grid>
            {attachments.map((attachment, index) => (
              <Grid.Col sm="6" key={index}>
                <MessageAttachment
                  attachment={attachment}
                  messageId={messageId}
                />
              </Grid.Col>
            ))}
          </Grid>
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
