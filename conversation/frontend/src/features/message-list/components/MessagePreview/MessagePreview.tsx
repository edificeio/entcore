import { useDate } from '@edifice.io/react';
import { IconPaperclip, IconUndo } from '@edifice.io/react/icons';
import { useTranslation } from 'react-i18next';
import { useMessageFolderId } from '~/hooks/useMessageFolderId';
import { MessageMetadata } from '~/models';

import RecipientAvatar from './components/RecipientAvatar';
import { SenderAvatar } from './components/SenderAvatar';
import { UserFolderIcon } from './components/UserFolderIcon';
import { useMessageUserDisplayName } from '~/hooks/useUserDisplayName';
import { MessageRecipientList } from '../../../../components/message-recipient-list';

export interface MessagePreviewProps {
  message: MessageMetadata;
}

export function MessagePreview({ message }: MessagePreviewProps) {
  const { t } = useTranslation('conversation');
  const { fromNow } = useDate();
  const senderDisplayName = useMessageUserDisplayName(message.from);
  const { messageFolderId, isInUserFolderOrTrash } =
    useMessageFolderId(message);
  if (!messageFolderId) return null;

  return (
    <div className="d-flex flex-fill gap-12 align-items-center  overflow-hidden fs-6">
      {message.response && (
        <IconUndo
          aria-hidden={false}
          className="gray-800"
          height={16}
          role="img"
          title={t('message.replied')}
          width={16}
        />
      )}

      {isInUserFolderOrTrash && messageFolderId && (
        <UserFolderIcon originFolderId={messageFolderId} />
      )}

      {['outbox', 'draft'].includes(messageFolderId) ? (
        <RecipientAvatar recipients={message.to} />
      ) : (
        <SenderAvatar authorId={message.from.id} />
      )}

      <div
        className="d-flex flex-fill flex-column overflow-hidden"
        tabIndex={0}
      >
        <div className="d-flex flex-fill justify-content-between overflow-hidden gap-4">
          {messageFolderId === 'draft' && (
            <span className="text-danger fw-bold">{t('draft')}</span>
          )}
          <div className="text-truncate flex-fill">
            {['outbox', 'draft'].includes(messageFolderId) ? (
              <div className="text-truncate">
                <MessageRecipientList message={message} inline />
              </div>
            ) : (
              <span>{senderDisplayName}</span>
            )}
          </div>

          {message.date && (
            <span className="text-nowrap caption fw-bold gray-800">
              {fromNow(message.date)}
            </span>
          )}
        </div>
        <div className="d-flex flex-fill justify-content-between overflow-hidden">
          {message.subject ? (
            <span className="text-truncate flex-fill">{message.subject}</span>
          ) : (
            <span className="text-truncate flex-fill text-gray-700">
              {t('nosubject')}
            </span>
          )}
          {message.hasAttachment && (
            <IconPaperclip
              aria-hidden={false}
              className="gray-800"
              height={16}
              role="img"
              title={t('message.has.attachment')}
              width={16}
            />
          )}
        </div>
      </div>
    </div>
  );
}
