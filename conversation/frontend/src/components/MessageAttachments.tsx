import { Attachment, Button, Grid, IconButton } from '@edifice.io/react';
import { IconDownload, IconFolderAdd, IconPlus } from '@edifice.io/react/icons';
import clsx from 'clsx';
import { useI18n } from '~/hooks';
import { Attachment as AttachmentMetaData } from '~/models';
import { baseUrl } from '~/services';
import './MessageAttachments.css';

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
            <div>{common_t('attachments')}</div>
            <div>
              <IconButton
                aria-label={common_t('conversation.copy.all.toworkspace')}
                color="tertiary"
                type="button"
                icon={<IconFolderAdd />}
                variant="ghost"
              />
              <IconButton
                aria-label={common_t('download.all.attachment')}
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
                <Attachment
                  name={attachment.filename}
                  options={
                    <>
                      <IconButton
                        aria-label={common_t('conversation.copy.toworkspace')}
                        color="tertiary"
                        type="button"
                        icon={<IconFolderAdd />}
                        variant="ghost"
                      />
                      <a
                        href={`${baseUrl}/message/${messageId}/attachement/${attachment.id}`}
                        download
                      >
                        <IconButton
                          aria-label={common_t('download.attachment')}
                          color="tertiary"
                          type="button"
                          icon={<IconDownload />}
                          variant="ghost"
                        />
                      </a>
                    </>
                  }
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
