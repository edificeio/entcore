import { useEdificeClient } from '@edifice.io/react';
import { BodyReplyOrTransfer } from '~/components/BodyReplyOrTransfer';
import { Message } from '~/models';
import { DEFAULT_MESSAGE, useMessage } from '~/services';
import { useI18n } from './useI18n';

export interface MessageReplyOrTransferProps {
  messageId: string | undefined;
  action: 'reply' | 'replyAll' | 'transfer';
}

export function useMessageReplyOrTransfer({
  messageId,
  action,
}: MessageReplyOrTransferProps) {
  const { data: messageOrigin } = useMessage(messageId || '');
  const { currentLanguage, user, userProfile } = useEdificeClient();
  const { t } = useI18n();

  if (messageOrigin?.id && action) {
    const messageTmp: Message = {
      ...DEFAULT_MESSAGE,
      language: currentLanguage,
    };

    const isTranferAction = action === 'transfer';

    const body = BodyReplyOrTransfer(messageOrigin, isTranferAction);
    messageTmp.body = body;

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
    return {
      message: messageTmp,
    };
  }
  return {
    message: messageOrigin,
  };
}
