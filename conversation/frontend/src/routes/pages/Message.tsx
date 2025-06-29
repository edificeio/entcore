import { QueryClient } from '@tanstack/react-query';
import { Fragment, useEffect, useState } from 'react';
import { LoaderFunctionArgs, useLoaderData } from 'react-router-dom';
import { MessageEdit } from '~/features/message-edit/MessageEdit';
import { Message } from '~/features/message/Message';
import { useInitMessage } from '~/hooks/useInitMessage';
import { useSelectedFolder } from '~/hooks/useSelectedFolder';
import { useMessageIdAndAction } from '~/hooks/useMessageIdAndAction';

import { messageQueryOptions, useFolderUtils } from '~/services';

export const loader =
  (queryClient: QueryClient, isPrint?: boolean) =>
  async ({ params /*, request*/ }: LoaderFunctionArgs) => {
    const queryMessage = messageQueryOptions.getById(
      params.messageId as string,
    );

    if (params.messageId) {
      await Promise.all([queryClient.ensureQueryData(queryMessage)]);
    }

    return { isPrint };
  };

export function Component() {
  const { isPrint } = useLoaderData() as { isPrint: boolean };
  const { folderId } = useSelectedFolder();
  const [currentKey, setCurrentKey] = useState(0);
  const { updateFolderMessagesQueryCache } = useFolderUtils();

  const { messageId, action } = useMessageIdAndAction();

  // Init message depending on the action

  const message = useInitMessage({
    messageId,
    action,
  });

  useEffect(() => {
    if (isPrint) {
      const timeoutId = setTimeout(() => window.print(), 1000);

      return () => clearTimeout(timeoutId);
    } else {
      // Scroll to the top of the page
      window.scrollTo(0, 0);
    }
  }, []);

  useEffect(() => {
    // Update the message unread status in the list
    if (folderId) {
      updateFolderMessagesQueryCache(folderId, (oldMessage) => {
        return oldMessage.id === messageId
          ? { ...oldMessage, unread: false }
          : oldMessage;
      });
    }
  }, [folderId]);

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
      {!isPrint && folderId === 'draft' && message.state === 'DRAFT' ? (
        <MessageEdit message={message} />
      ) : (
        <Message message={message} isPrint={isPrint} />
      )}
    </Fragment>
  );
}
