import { QueryClient } from '@tanstack/react-query';
import { Fragment, useEffect, useState } from 'react';
import { LoaderFunctionArgs } from 'react-router-dom';
import { MessageEdit } from '~/features/message-edit/MessageEdit';
import { Message } from '~/features/message/Message';
import { useInitMessage, useSelectedFolder } from '~/hooks';
import { useMessageIdAndAction } from '~/hooks/useMessageIdAndAction';

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
  const { folderId } = useSelectedFolder();
  const [currentKey, setCurrentKey] = useState(0);

  const { messageId, action } = useMessageIdAndAction();

  // Init message depending on the action

  const message = useInitMessage({
    messageId,
    action,
  });

  useEffect(() => {
    // Scroll to the top of the page
    window.scrollTo(0, 0);
  }, []);

  useEffect(() => {
    if (messageId === message?.id) {
      // Update the current key to trigger a re-render
      setCurrentKey((prev) => prev + 1);
    }
  }, [messageId, message?.id]);

  if (!message) {
    return null;
  }

  return (
    <Fragment key={currentKey}>
      {folderId === 'draft' && message.state === 'DRAFT' ? (
        <MessageEdit message={message} />
      ) : (
        <Message message={message} />
      )}
    </Fragment>
  );
}
