import { useDate, useEdificeClient } from '@edifice.io/react';
import { Message, Recipients } from '~/models';
import { DEFAULT_MESSAGE, useMessage } from '~/services';
import { useI18n } from './useI18n';

export interface MessageReplyOrTransferProps {
  messageId: string | undefined;
  replyMessageId?: string | null;
  replyAllMessageId?: string | null;
  transferMessageId?: string | null;
}

export function useMessageReplyOrTransfer({
  messageId,
  replyMessageId,
  replyAllMessageId,
  transferMessageId,
}: MessageReplyOrTransferProps) {
  const { data: messageOrigin } = useMessage({
    messageId:
      messageId ||
      replyMessageId ||
      replyAllMessageId ||
      transferMessageId ||
      '',
  });
  const { currentLanguage, user, userProfile } = useEdificeClient();
  const { t } = useI18n();
  const { formatDate } = useDate();
  const displayRecipient = (recipients: Recipients) => {
    const usersDisplayName = recipients.users
      .map((user) => user.displayName)
      .join(', ');
    const groupsDisplayName = recipients.groups
      .map((group) => group.displayName)
      .join(', ');
    return (
      usersDisplayName +
      (recipients.users.length > 0 && recipients.groups.length > 0
        ? ', '
        : '') +
      groupsDisplayName
    );
  };

  if (
    messageOrigin?.id &&
    (!!replyMessageId || !!replyAllMessageId || !!transferMessageId)
  ) {
    const messageTmp: Message = {
      ...DEFAULT_MESSAGE,
      language: currentLanguage,
    };

    const body = `<p></p><p></p>
          <div class="${transferMessageId ? '' : 'conversation-history'}">
              ${transferMessageId ? '<p><span style="font-size: 14px; font-weight:400;">' + t('transfer.title') + '</span></p>' : ''}
              <p><span style="font-size: 14px; font-weight:400;">${t('transfer.from') + messageOrigin.from?.displayName}</span></p>
              <p><span style="font-size: 14px; font-weight:400;">${t('transfer.date') + (!messageOrigin.date ? '' : formatDate(messageOrigin.date, 'long'))}</span></p>
              <p><span style="font-size: 14px; font-weight:400;">${t('transfer.subject') + messageOrigin.subject}</span></p>
              <p><span style="font-size: 14px; font-weight:400;">${t('transfer.to') + displayRecipient(messageOrigin.to)}</span></p>
              ${messageOrigin.cc.users.length || messageOrigin.cc.groups.length ? '<p><span style="font-size: 14px; font-weight:400;">' + t('transfer.cc') + displayRecipient(messageOrigin.cc) + '</span></p>' : ''}
            <p class="${transferMessageId ? '' : 'conversation-history-body'}">
              ${messageOrigin.body}
            </p>
          </div>`;
    messageTmp.body = body;

    if (replyMessageId) {
      messageTmp.to.users = [messageOrigin.from];
      messageTmp.to.groups = [];
      messageTmp.cc.users = [];
      messageTmp.cc.groups = [];
      messageTmp.cci = undefined;
    } else if (replyAllMessageId) {
      messageTmp.to = {
        ...messageOrigin.to,
        users: [
          ...messageOrigin.to.users.filter(
            (user) => user.id !== messageOrigin.from.id,
          ),
          messageOrigin.from,
        ],
      };
      messageTmp.cc = { ...messageOrigin.cc };
      if (messageOrigin.from.id === user?.userId && messageOrigin.cci) {
        messageTmp.cci = { ...messageOrigin.cci };
      }
    } else if (transferMessageId) {
      messageTmp.to.users = [];
      messageTmp.to.groups = [];
      messageTmp.cc.users = [];
      messageTmp.cc.groups = [];
      messageTmp.cci = undefined;
    }

    messageTmp.from = {
      id: user?.userId || '',
      displayName: user?.username || '',
      profile: (userProfile || '') as string,
    };

    const prefixSubject =
      replyMessageId || replyAllMessageId
        ? t('message.reply.subject')
        : t('message.transfer.subject');

    if (!messageOrigin.subject.startsWith(prefixSubject)) {
      messageTmp.subject = `${prefixSubject}${messageOrigin.subject}`;
    }
    return {
      message: messageTmp,
    };
  }
  return {
    message: messageOrigin,
  };
}
