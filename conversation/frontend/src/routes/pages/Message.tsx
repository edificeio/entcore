import { QueryClient } from '@tanstack/react-query';
import { Fragment, useEffect, useState } from 'react';
import { LoaderFunctionArgs, useParams } from 'react-router-dom';
import { Message } from '~/features/message';
import { MessageEdit } from '~/features/message-edit/MessageEdit';
import { useSelectedFolder } from '~/hooks';

import { messageQueryOptions, useMessage } from '~/services';

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
  const { folderId } = useSelectedFolder();
  const [currentKey, setCurrentKey] = useState(0);

  const { data: message } = useMessage(messageId!);

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
