import { useParams, useSearchParams } from 'react-router-dom';
import { UserAction } from './useInitMessage';

export interface MessageIdAndActionReturn {
  messageId: string | undefined;
  action?: UserAction;
  transferMessageId?: string;
}

export function useMessageIdAndAction(): MessageIdAndActionReturn {
  const { messageId } = useParams();
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
    transferMessageId: transferMessageId || undefined,
  };
}
