import { Button, Dropzone, DropzoneContext, IconButton, useDropzone } from '@edifice.io/react';
import { IconDownload, IconFolderAdd, IconPlus } from '@edifice.io/react/icons';
import clsx from 'clsx';
import { useI18n } from '~/hooks';
import { Attachment as AttachmentMetaData } from '~/models';
import { baseUrl } from '~/services';
import { MessageAttachment } from './MessageAttachment';
import './index.css';
import { useMemo } from 'react';

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


  const {
    inputRef,
//    dragging,
    files,
    addFile,
    deleteFile,
    replaceFileAt,
    handleDragLeave,
    handleDragging,
    handleDrop,
    handleOnChange,
  } = useDropzone();

  const value = useMemo(
    () => ({
      inputRef,
      files,
      addFile,
      deleteFile,
      replaceFileAt,
    }),
    [addFile, deleteFile, replaceFileAt, files, inputRef],
  );

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
          <ul className="d-flex gap-8 flex-wrap list-unstyled m-0">
            {attachments.map((attachment) => (
              <li key={attachment.id}>
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
        <DropzoneContext.Provider value={value}>
          <div
            onDragEnter={handleDragging}
            onDragOver={handleDragging}
            onDragLeave={handleDragLeave}
            onDrop={handleDrop}
          >
            <Button color="secondary" variant="ghost" leftIcon={<IconPlus />} onClick={() => inputRef?.current?.click()}>
              {t('add.attachment')}
            </Button>
            <input
              ref={inputRef}
              multiple={true}
              type="file"
              name="attachment-input"
              id="attachment-input"
              onChange={handleOnChange}
              hidden
            />
          </div>
        </DropzoneContext.Provider>
      )}
    </div>
  );
}
