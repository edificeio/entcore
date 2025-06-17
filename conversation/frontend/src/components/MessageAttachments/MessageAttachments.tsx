import { Button, IconButton } from '@edifice.io/react';
import {
  IconDelete,
  IconDownload,
  IconFolderAdd,
  IconPlus,
} from '@edifice.io/react/icons';
import clsx from 'clsx';
import { ChangeEvent, useRef, useState } from 'react';
import { useI18n } from '~/hooks/useI18n';
import { useMessageAttachments } from '~/hooks/useMessageAttachments';
import { Attachment, Message } from '~/models';
import { AddMessageAttachmentToWorkspaceModal } from './components/AddMessageAttachmentToWorkspaceModal';
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
  const { downloadAllUrl, attachFiles, detachFiles, isMutating } =
    useMessageAttachments();
  const [attachmentsToAddToWorkspace, setAttachmentsToAddToWorkspace] =
    useState<Attachment[] | undefined>(undefined);

  const attachments = message.attachments;

  if (!editMode && !attachments.length) return null;

  const handleAttachClick = () => inputRef?.current?.click();

  const handleFileChange = (event: ChangeEvent<HTMLInputElement>) => {
    attachFiles(event.target.files);
  };

  const handleDetachAllClick = () => detachFiles(attachments);

  const className = clsx(
    'bg-gray-200 rounded-2 px-12 py-8 message-attachments align-self-start gap-8 d-flex flex-column',
    { 'border message-attachments-edit mx-16': editMode },
  );

  const handleWantAddToWorkspace = (attachments: Attachment[]) => {
    setAttachmentsToAddToWorkspace(attachments);
  };
  return (
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
                  title={t('conversation.copy.all.toworkspace')}
                  color="tertiary"
                  type="button"
                  icon={<IconFolderAdd />}
                  onClick={() => handleWantAddToWorkspace(attachments)}
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
          <ul className="d-flex gap-8 flex-column list-unstyled m-0">
            {attachments.map((attachment) => (
              <li key={attachment.id} className="mw-100">
                <MessageAttachment
                  attachment={attachment}
                  editMode={editMode}
                  onWantAddToWorkspace={(attachment) =>
                    handleWantAddToWorkspace([attachment])
                  }
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
            isLoading={isMutating}
            onClick={handleAttachClick}
            disabled={isMutating}
            className="align-self-start"
            leftIcon={<IconPlus />}
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
      {!!attachmentsToAddToWorkspace && (
        <AddMessageAttachmentToWorkspaceModal
          isOpen
          onModalClose={() => setAttachmentsToAddToWorkspace(undefined)}
          attachments={attachmentsToAddToWorkspace}
        />
      )}
    </div>
  );
}
