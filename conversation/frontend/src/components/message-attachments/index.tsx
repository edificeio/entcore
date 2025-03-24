import { Button, IconButton } from '@edifice.io/react';
import {
  IconDownload,
  IconFolderAdd,
  IconLoader,
  IconPlus,
} from '@edifice.io/react/icons';
import clsx from 'clsx';
import { useI18n } from '~/hooks';
import { Message } from '~/models';
import { MessageAttachment } from './MessageAttachment';
import './index.css';
import { ChangeEvent, useRef } from 'react';
import { useMessageAttachments } from '~/hooks/useMessageAttachments';

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
  const { attachments, downloadAllUrl, attachFiles, isMutating } =
    useMessageAttachments(message);

  if (!attachments.length && !editMode) return null;

  const handleAttachClick = () => inputRef?.current?.click();

  const handleFileChange = (event: ChangeEvent<HTMLInputElement>) => {
    attachFiles(event.target.files);
  };

  const className = clsx(
    'bg-gray-300 rounded-2 px-12 py-8 message-attachments gap-8 d-flex flex-column',
    editMode && 'border message-attachments-edit mx-16',
  );

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
                <a href={downloadAllUrl} download>
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
          <ul className="d-flex gap-8 flex-wrap list-unstyled m-0">
            {attachments.map((attachment) => (
              <li key={attachment.id}>
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
  );
}
