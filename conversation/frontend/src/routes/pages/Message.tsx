import { QueryClient } from '@tanstack/react-query';
import { Fragment, useEffect, useState } from 'react';
import {
  LoaderFunctionArgs,
  useParams,
  useSearchParams,
} from 'react-router-dom';
import { Message } from '~/features/message';
import { MessageEdit } from '~/features/message-edit/MessageEdit';
import { useSelectedFolder } from '~/hooks';
import { useMessageReplyOrTransfer } from '~/hooks/useMessageReplyOrTransfer';

import { messageQueryOptions } from '~/services';

export const loader =
  (queryClient: QueryClient) =>
  async ({ params /*, request*/ }: LoaderFunctionArgs) => {
    const queryMessage = messageQueryOptions.getById(
      params.messageId as string,
    );
    if (params.messageId) {
      await Promise.all([queryClient.ensureQueryData(queryMessage)]);
    }

    return null;
  };

export function Component() {
  const { messageId } = useParams();
  const [searchParams] = useSearchParams();
  const { folderId } = useSelectedFolder();
  const [currentKey, setCurrentKey] = useState(0);

  const replyMessageId = searchParams.get('reply');
  const replyAllMessageId = searchParams.get('replyall');
  const transferMessageId = searchParams.get('transfer');
  const { message } = useMessageReplyOrTransfer({
    messageId,
    replyMessageId,
    replyAllMessageId,
    transferMessageId,
  });

  useEffect(() => {
    // Scroll to the top of the page
    window.scrollTo(0, 0);
  }, []);

  useEffect(() => {
    // Update the current key to trigger a re-render
    setCurrentKey((prev) => prev + 1);
  }, [messageId]);

  if (!message) {
    return null;
  }

  return (
    <Fragment key={currentKey}>
      {folderId === 'draft' && message?.state === 'DRAFT' ? (
        <MessageEdit message={message} />
      ) : (
        <Message message={message} />
      )}
    </Fragment>
  );
}
