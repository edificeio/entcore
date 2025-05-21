import { useDate, useEdificeClient } from '@edifice.io/react';
import { useLayoutEffect, useMemo } from 'react';
import { Group, Message, Recipients, User } from '~/models';
import {
  createDefaultMessage,
  useMessageQuery,
  useSignaturePreferences,
} from '~/services';
import { useMessageActions } from '~/store/messageStore';
import { useAdditionalRecipients } from './useAdditionalRecipients';
import { useI18n } from './useI18n';

export type UserAction = 'reply' | 'replyAll' | 'transfer';
export interface MessageReplyOrTransferProps {
  messageId: string | undefined;
  action?: UserAction;
}

export function useInitMessage({
  messageId,
  action,
}: MessageReplyOrTransferProps) {
  const { data: messageOrigin } = useMessageQuery(messageId || '');
  const { currentLanguage, user, userProfile } = useEdificeClient();
  const { t, common_t } = useI18n();
  const { formatDate } = useDate();
  const { data: signatureData, isPending: getSignatureIsPending } =
    useSignaturePreferences();
  const { setMessage } = useMessageActions();

  // Get IDs of users and groups/favorites to add as recipients.
  const { recipients: recipientsToAddToMessage } = useAdditionalRecipients();

  const signature =
    signatureData?.useSignature && signatureData.signature
      ? `<p></p><p></p>${signatureData.signature}`
      : '';

  /**
   * Creates and formats a message based on the action type (reply, replyAll, or transfer) and original message.
   *
   * @returns {Message | undefined} A formatted message object with appropriate recipients, body content and subject
   * based on the action type. Returns undefined if signature is pending or no message origin exists.
   *
   * The returned message includes:
   * - Signature (if enabled in settings)
   * - Original message content formatted according to action type
   * - Recipients set based on action:
   *   - reply: only original sender
   *   - replyAll: original sender + all recipients except current user
   *   - transfer: empty recipients list but keeps attachments
   * - Subject prefixed with appropriate text based on action type
   * - Thread/parent IDs linked to original message
   *
   * @remarks
   * - For transfers: includes detailed original message metadata (sender, date, subject, recipients)
   * - For replies: includes quoted original message with sender info and timestamp
   * - Handles CCI recipients only for reply-all when user was original sender
   */
  const message = useMemo(() => {
    // If the configuration for the signature is pending, we return an empty message
    if (getSignatureIsPending || !messageOrigin) {
      return undefined;
    }
    let messageTmp: Message = messageOrigin;

    if (messageOrigin.id && action) {
      // We are in the case of a reply, replayAll or transfer
      messageTmp = {
        ...createDefaultMessage(signature),
        language: currentLanguage,
        parent_id: messageOrigin.id,
        thread_id: messageOrigin.id,
        from: {
          id: user?.userId || '',
          displayName: user?.username || '',
          profile: (userProfile || '') as string,
        },
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

      let body = `${signature}`;
      if (action === 'transfer') {
        body =
          body +
          `<div>
            ${signatureData?.useSignature ? `<p></p>` : ''}
            <p><span style="font-size: 14px; font-weight:400;">--------- ${t('transfer.title')} ---------</span></p>
            <p><span style="font-size: 14px; font-weight:400;">${t('transfer.from') + messageOrigin.from?.displayName}</span></p>
            <p><span style="font-size: 14px; font-weight:400;">${t('transfer.date') + (messageOrigin.date ? formatDate(messageOrigin.date, 'LLL') : '')}</span></p>
            <p><span style="font-size: 14px; font-weight:400;">${t('transfer.subject') + messageOrigin.subject}</span></p>
            <p><span style="font-size: 14px; font-weight:400;">${t('transfer.to') + displayRecipient(messageOrigin.to)}</span></p>
            ${messageOrigin.cc.users.length || messageOrigin.cc.groups.length ? '<p><span style="font-size: 14px; font-weight:400;">' + t('transfer.cc') + displayRecipient(messageOrigin.cc) + '</span></p>' : ''}
            ${messageOrigin.body}
        </div>`;
        messageTmp.to.users = [];
        messageTmp.to.groups = [];
        messageTmp.cc.users = [];
        messageTmp.cc.groups = [];
        messageTmp.cci = undefined;
        messageTmp.attachments = messageOrigin.attachments;
      } else {
        body =
          body +
          `<div class="conversation-history">
          <p><span style="font-size: 14px; font-weight:400;"><em>${t('from') + ' ' + messageOrigin.from?.displayName + (messageOrigin.date ? ', ' + common_t('date.format.pretty', { date: formatDate(messageOrigin.date, 'LL'), time: formatDate(messageOrigin.date, 'LT') }) : '')}</em></span></p>
          <p><span style="font-size: 14px; font-weight:400; color: #909090;"><em>${t('transfer.to') + displayRecipient(messageOrigin.to)}</em></span></p>
          ${messageOrigin.cc.users.length || messageOrigin.cc.groups.length ? '<p><span style="font-size: 14px; font-weight:400;color: #909090;"><em>' + t('transfer.cc') + displayRecipient(messageOrigin.cc) + '</em></span></p>' : ''}
          <div class="conversation-history-body">
            ${messageOrigin.body}
          </div>
        </div>`;

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
                  (user: User) => user.id !== messageOrigin.from.id,
                ),
                messageOrigin.from,
              ],
            };
            messageTmp.cc = { ...messageOrigin.cc };
            if (messageOrigin.from.id === user?.userId && messageOrigin.cci) {
              messageTmp.cci = { ...messageOrigin.cci };
            }
            break;
        }
      }

      messageTmp.body = body;

      const prefixSubject =
        action === 'transfer'
          ? t('message.transfer.subject')
          : t('message.reply.subject');

      if (!messageOrigin.subject.startsWith(prefixSubject)) {
        messageTmp.subject = `${prefixSubject}${messageOrigin.subject}`;
      }

      if (recipientsToAddToMessage) {
        messageTmp.to.users = [
          ...recipientsToAddToMessage.users,
          ...messageTmp.to.users.filter((user: User) =>
            recipientsToAddToMessage.users.some((u) => u.id === user.id),
          ),
        ];
        messageTmp.to.groups = [
          ...recipientsToAddToMessage.groups,
          ...messageOrigin.to.groups.filter((group: Group) =>
            recipientsToAddToMessage.groups.some((g) => g.id === group.id),
          ),
        ];
      }
    } else {
      if (!messageOrigin.id) {
        // We are in the case of a new message
        // We are in the case of a reply, replayAll or transfer
        messageTmp = {
          ...createDefaultMessage(signature),
          language: currentLanguage,
          from: {
            id: user?.userId || '',
            displayName: user?.username || '',
            profile: (userProfile || '') as string,
          },
        };
        if (signatureData?.useSignature && signatureData.signature) {
          messageTmp.body = `${signature}`;
        } else {
          messageTmp.body = '';
        }

        if (
          recipientsToAddToMessage?.groups.length ||
          recipientsToAddToMessage?.users.length
        ) {
          messageTmp.to = {
            users: [...(recipientsToAddToMessage?.users || [])],
            groups: [...(recipientsToAddToMessage?.groups || [])],
          };
        }
      }
    }

    return messageTmp;
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [
    getSignatureIsPending,
    messageOrigin,
    messageId,
    action,
    recipientsToAddToMessage,
  ]);

  useLayoutEffect(() => {
    if (message) {
      setMessage({ ...message });
    }

    return () => {
      setMessage(createDefaultMessage(signature));
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [message, getSignatureIsPending, signatureData]);
}
