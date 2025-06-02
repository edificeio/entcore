import { QueryClient } from '@tanstack/react-query';
import { Fragment, useEffect, useState } from 'react';
import { LoaderFunctionArgs, useLoaderData } from 'react-router-dom';
import { MessageEdit } from '~/features/message-edit/MessageEdit';
import { Message } from '~/features/message/Message';
import { useInitMessage } from '~/hooks/useInitMessage';
import { useMessageIdAndAction } from '~/hooks/useMessageIdAndAction';
import { useSelectedFolder } from '~/hooks/useSelectedFolder';

import { messageQueryOptions } from '~/services';
import { useAppActions } from '~/store';

export const loader =
  (queryClient: QueryClient, isPrint?: boolean) =>
  async ({ params /*, request*/ }: LoaderFunctionArgs) => {
    const queryMessage = messageQueryOptions.getById(
      params.messageId as string,
    );

    if (params.messageId) {
      const loadPromise = new Promise((resolve) => {
        setTimeout(() => {
          resolve(true);
        }, 2000);
      });
      return {
        isPrint,
        nonCriticalPromise: Promise.all([
          queryClient.ensureQueryData(queryMessage),
          loadPromise,
        ]),
      };
    }

    return { isPrint };
  };

export function Component() {
  const { isPrint, nonCriticalPromise } = useLoaderData() as {
    isPrint: boolean;
    nonCriticalPromise?: Promise<void>;
  };
  const { folderId } = useSelectedFolder();
  const [currentKey, setCurrentKey] = useState(0);
  const { setIsLoading } = useAppActions();

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
    if (messageId === message?.id) {
      // Update the current key to trigger a re-render
      setCurrentKey((prev) => prev + 1);
    }
  }, [messageId, message?.id]);

  if (nonCriticalPromise) {
    setIsLoading(true);
    nonCriticalPromise.then(() => {
      // setIsLoading(false);
    });
  }

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
