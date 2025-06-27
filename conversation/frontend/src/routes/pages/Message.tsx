import { QueryClient } from '@tanstack/react-query';
import { Fragment, useEffect, useState } from 'react';
import { LoaderFunctionArgs, useLoaderData } from 'react-router-dom';
import { MessageEdit } from '~/features/message-edit/MessageEdit';
import { MessageEditSkeleton } from '~/features/message-edit/MessageEditSkeleton';
import { Message } from '~/features/message/Message';
import { MessageSkeleton } from '~/features/message/MessageSkeleton';
import { useInitMessage } from '~/hooks/useInitMessage';
import { useMessageIdAndAction } from '~/hooks/useMessageIdAndAction';
import { useSelectedFolder } from '~/hooks/useSelectedFolder';

import { messageQueryOptions, useFolderUtils } from '~/services';

export const loader =
  (queryClient: QueryClient, isPrint?: boolean) =>
  async ({ params /*, request*/ }: LoaderFunctionArgs) => {
    const queryMessage = messageQueryOptions.getById(
      params.messageId as string,
    );

    queryClient.ensureQueryData(queryMessage);

    if (params.messageId) {
      return {
        isPrint,
      };
    }

    return { isPrint };
  };

export function Component() {
  const { isPrint } = useLoaderData() as {
    isPrint: boolean;
  };
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
    // eslint-disable-next-line react-hooks/exhaustive-deps
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
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [folderId]);

  useEffect(() => {
    if (messageId === message?.id) {
      // Update the current key to trigger a re-render
      setCurrentKey((prev) => prev + 1);
    }
  }, [messageId, message?.id]);

  if (!message) {
    return (
      <>
        {folderId === 'draft' ? <MessageEditSkeleton /> : <MessageSkeleton />}
      </>
    );
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
