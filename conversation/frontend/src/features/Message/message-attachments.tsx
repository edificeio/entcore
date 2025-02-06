import { Attachment, Grid, IconButton } from '@edifice.io/react';
import { IconDownload, IconFolderAdd } from '@edifice.io/react/icons';
import { useI18n } from '~/hooks';
import { Attachment as AttachmentMetaData } from '~/models';
import { baseUrl } from '~/services';

export interface MessageAttachmentsProps {
  attachments: AttachmentMetaData[];
  messageId: string;
}

export function MessageAttachments({
  attachments,
  messageId,
}: MessageAttachmentsProps) {
  const { common_t } = useI18n();

  return (
    <>
      {!!attachments.length && (
        <div
          className="mt-16 bg-gray-300 rounded-2 px-12 py-8"
          data-drag-handle
        >
          <div className="d-flex align-items-center justify-content-between mb-8 mt-0 border-bottom">
            <div className="">{common_t('attachments')}</div>
            <div className="">
              <IconButton
                aria-label={common_t('conversation.copy.all.toworkspace')}
                color="tertiary"
                type="button"
                icon={<IconFolderAdd />}
                variant="ghost"
              />
              <a download>
                <IconButton
                  aria-label={common_t('download.all.attachment')}
                  color="tertiary"
                  type="button"
                  icon={<IconDownload />}
                  variant="ghost"
                />
              </a>
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
        </div>
      )}
    </>
  );
}
