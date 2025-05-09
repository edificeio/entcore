import { useDate, useEdificeClient } from '@edifice.io/react';
import { Message, Recipients } from '~/models';
import {
  DEFAULT_MESSAGE,
  useMessage,
  useSignaturePreferences,
} from '~/services';
import { useI18n } from './useI18n';
import { useMemo } from 'react';

export interface MessageReplyOrTransferProps {
  messageId: string | undefined;
  action?: 'reply' | 'replyAll' | 'transfer';
}

export function useMessageReplyOrTransfer({
  messageId,
  action,
}: MessageReplyOrTransferProps) {
  const { data: messageOrigin } = useMessage(messageId || '');
  const { currentLanguage, user, userProfile } = useEdificeClient();
  const { t } = useI18n();
  const { formatDate } = useDate();
  const { data: signatureData, isPending: getSignatureIsPending } =
    useSignaturePreferences();

  const message = useMemo(() => {
    // If the configuration for the signature is pending, we return an empty message
    if (getSignatureIsPending || !messageOrigin) {
      return undefined;
    }

    if (messageOrigin.id && action) {
      const messageTmp: Message = {
        ...DEFAULT_MESSAGE,
        language: currentLanguage,
      };
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

      const isTranferAction = action === 'transfer';

      const body = `<p></p><p></p>
      ${signatureData?.useSignature ? signatureData.signature : ''}
      <div class="${isTranferAction ? '' : 'conversation-history'}">
          ${isTranferAction ? '<p><span style="font-size: 14px; font-weight:400;">' + t('transfer.title') + '</span></p>' : ''}
          <p><span style="font-size: 14px; font-weight:400;">${t('transfer.from') + messageOrigin.from?.displayName}</span></p>
          <p><span style="font-size: 14px; font-weight:400;">${t('transfer.date') + (!messageOrigin.date ? '' : formatDate(messageOrigin.date, 'long'))}</span></p>
          <p><span style="font-size: 14px; font-weight:400;">${t('transfer.subject') + messageOrigin.subject}</span></p>
          <p><span style="font-size: 14px; font-weight:400;">${t('transfer.to') + displayRecipient(messageOrigin.to)}</span></p>
          ${messageOrigin.cc.users.length || messageOrigin.cc.groups.length ? '<p><span style="font-size: 14px; font-weight:400;">' + t('transfer.cc') + displayRecipient(messageOrigin.cc) + '</span></p>' : ''}
        <p class="${isTranferAction ? '' : 'conversation-history-body'}">
          ${messageOrigin.body}
        </p>
      </div>`;
      messageTmp.body = body;
      messageTmp.attachments = messageOrigin.attachments;

      switch (action) {
        case 'reply':
          messageTmp.to.users = [messageOrigin.from];
          messageTmp.to.groups = [];
          messageTmp.cc.users = [];
          messageTmp.cc.groups = [];
          messageTmp.cci = undefined;
          break;
        case 'replyAll':
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
          break;
        case 'transfer':
          messageTmp.to.users = [];
          messageTmp.to.groups = [];
          messageTmp.cc.users = [];
          messageTmp.cc.groups = [];
          messageTmp.cci = undefined;
          break;
      }

      messageTmp.from = {
        id: user?.userId || '',
        displayName: user?.username || '',
        profile: (userProfile || '') as string,
      };

      const prefixSubject =
        action === 'transfer'
          ? t('message.transfer.subject')
          : t('message.reply.subject');

      if (!messageOrigin.subject.startsWith(prefixSubject)) {
        messageTmp.subject = `${prefixSubject}${messageOrigin.subject}`;
      }
      return messageTmp;
    } else {
      if (!messageOrigin.id) {
        messageOrigin.from = {
          id: user?.userId || '',
          displayName: user?.username || '',
          profile: (userProfile || '') as string,
        };
        messageOrigin.to = {
          users: [],
          groups: [],
        };
        messageOrigin.cc = {
          users: [],
          groups: [],
        };
        messageOrigin.cci = undefined;
        messageOrigin.subject = '';

        if (signatureData?.useSignature && signatureData.signature) {
          messageOrigin.body = `<p></p><p></p>${signatureData.signature}`;
        }
      }
      return messageOrigin;
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [getSignatureIsPending, messageOrigin, messageId, action]);

  return { message };
}
