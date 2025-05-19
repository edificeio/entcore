import { useSearchParams } from 'react-router-dom';
import { UserAction } from './useMessageReplyOrTransfer';

export interface MessageIdAndActionReturn {
  messageId: string | undefined;
  action?: UserAction;
}

export function useMessageIdAndAction(
  messageId: string | undefined,
): MessageIdAndActionReturn {
  const [searchParams] = useSearchParams();
  const replyMessageId = searchParams.get('reply');
  const replyAllMessageId = searchParams.get('replyall');
  const transferMessageId = searchParams.get('transfer');

  return {
    messageId:
      messageId ||
      replyMessageId ||
      replyAllMessageId ||
      transferMessageId ||
      '',
    action: replyMessageId
      ? 'reply'
      : replyAllMessageId
        ? 'replyAll'
        : transferMessageId
          ? 'transfer'
          : undefined,
  };
}
