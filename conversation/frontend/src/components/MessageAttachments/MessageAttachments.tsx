import { Button, IconButton } from '@edifice.io/react';
import {
  IconDelete,
  IconDownload,
  IconFolderAdd,
  IconLoader,
  IconPlus,
} from '@edifice.io/react/icons';
import clsx from 'clsx';
import { ChangeEvent, useRef } from 'react';
import { useI18n } from '~/hooks';
import { useMessageAttachments } from '~/hooks/useMessageAttachments';
import { Message } from '~/models';
import { useMessageUpdated } from '~/store';
import { MessageAttachment } from './components/MessageAttachment';
import './MessageAttachments.css';

export interface MessageAttachmentsProps {
  message: Message;
  editMode?: boolean;
}

export function MessageAttachments({
  message,
  editMode = false,
}: MessageAttachmentsProps) {
  const { common_t, t } = useI18n();
  const inputRef = useRef<HTMLInputElement | null>(null);
  const messageUpdated = useMessageUpdated();
  const { attachments, downloadAllUrl, attachFiles, detachFiles, isMutating } =
    useMessageAttachments(
      editMode && messageUpdated ? messageUpdated : message,
    );

  if (!attachments.length && !editMode) return null;

  const handleAttachClick = () => inputRef?.current?.click();

  const handleFileChange = (event: ChangeEvent<HTMLInputElement>) => {
    attachFiles(event.target.files);
  };

  const handleDetachAllClick = () => detachFiles(attachments);

  const className = clsx(
    'bg-gray-300 rounded-2 px-12 py-8 message-attachments align-self-start gap-8 d-flex flex-column',
    editMode && 'border message-attachments-edit mx-16',
  );

  return (
    message.state !== 'RECALL' && (
      <div
        className={className}
        style={{ maxWidth: '-webkit-fill-available' }}
        data-drag-handle
      >
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
                  <a href={downloadAllUrl} download>
                    <IconButton
                      title={common_t('download.all.attachment')}
                      color="tertiary"
                      type="button"
                      icon={<IconDownload />}
                      variant="ghost"
                    />
                  </a>
                  {editMode && (
                    <IconButton
                      title={t('remove.all.attachment')}
                      color="danger"
                      type="button"
                      icon={<IconDelete />}
                      variant="ghost"
                      onClick={handleDetachAllClick}
                      disabled={isMutating}
                    />
                  )}
                </div>
              )}
            </div>
            <ul className="d-flex gap-8 flex-wrap list-unstyled m-0">
              {attachments.map((attachment) => (
                <li key={attachment.id} className="mw-100">
                  <MessageAttachment
                    attachment={attachment}
                    message={message}
                    editMode={editMode}
                  />
                </li>
              ))}
            </ul>
          </>
        )}
        {editMode && (
          <>
            <Button
              color="secondary"
              variant="ghost"
              leftIcon={isMutating ? <IconLoader /> : <IconPlus />}
              onClick={handleAttachClick}
              disabled={isMutating}
            >
              {t('add.attachment')}
            </Button>
            <input
              ref={inputRef}
              multiple={true}
              type="file"
              name="attachment-input"
              id="attachment-input"
              onChange={handleFileChange}
              hidden
            />
          </>
        )}
      </div>
    )
  );
}
