import { Avatar, useDate, useDirectory } from '@edifice.io/react';
import {
  IconGroupAvatar,
  IconPaperclip,
  IconUndo,
} from '@edifice.io/react/icons';
import { useTranslation } from 'react-i18next';
import { useParams } from 'react-router-dom';
import { MessageMetadata, Recipients } from '~/models';
import { MessageRecipientList } from './message-recipient-list';

export interface MessagePreviewProps {
  message: MessageMetadata;
}

export function MessagePreview({ message }: MessagePreviewProps) {
  const { t } = useTranslation('conversation');
  const { folderId } = useParams<{ folderId: string }>();
  const { getAvatarURL } = useDirectory();
  const { fromNow } = useDate();

  return (
    <div className="d-flex gap-12 align-items-center flex-fill overflow-hidden fs-6">
      {(message.response || message.forwarded) && (
        <IconUndo className="gray-800" title="message-response" />
      )}

      {'outbox' === folderId ? (
        <RecipientAvatar recipients={message.to} />
      ) : (
        <Avatar
          alt={t('author.avatar')}
          size="sm"
          src={getAvatarURL(message.from.id, 'user')}
          variant="circle"
        />
      )}
      <div className="d-flex flex-fill flex-column overflow-hidden">
        <div className="d-flex flex-fill justify-content-between overflow-hidden">
          <div className="text-truncate flex-fill">
            {'outbox' === folderId ? (
              <MessageRecipientList
                head={t('at')}
                recipients={message.to}
                color="text-gray-800"
                truncate
                linkDisabled
              />
            ) : (
              message.from.displayName
            )}
          </div>
          <div className="fw-bold text-nowrap fs-12 gray-800">
            {fromNow(message.date)}
          </div>
        </div>
        <div className="d-flex flex-fill justify-content-between overflow-hidden">
          {message.subject ? (
            <div className="text-truncate flex-fill">{message.subject}</div>
          ) : (
            <div className="text-truncate flex-fill text-gray-700">
              {t('nosubject')}
            </div>
          )}
          {message.hasAttachment && (
            <IconPaperclip
              className="gray-800"
              height={16}
              width={16}
              title="message-has-attachment"
            />
          )}
        </div>
      </div>
    </div>
  );
}

const RecipientAvatar = ({ recipients }: { recipients: Recipients }) => {
  const { t } = useTranslation('conversation');
  const { getAvatarURL } = useDirectory();
  const recipientLength = recipients.users.length + recipients.groups.length;

  if (recipientLength > 1) {
    return (
      <div className="bg-orange-200 avatar avatar-sm rounded-circle">
        <IconGroupAvatar
          className="w-16"
          aria-label={t('recipient.avatar.group')}
          role="img"
        />
      </div>
    );
  } else {
    const firstRecipient = recipients.users[0] || recipients.groups[0];
    const firstRecipientType = recipients.users.length > 0 ? 'user' : 'group';
    const url = getAvatarURL(firstRecipient.id, firstRecipientType);
    return (
      <Avatar
        alt={t('recipient.avatar')}
        size="sm"
        src={url}
        variant="circle"
      />
    );
  }
};
