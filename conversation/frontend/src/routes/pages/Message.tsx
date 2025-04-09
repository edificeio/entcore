import { QueryClient } from '@tanstack/react-query';
import { useEffect } from 'react';
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

    await Promise.all([queryClient.ensureQueryData(queryMessage)]);

    return null;
  };

export function Component() {
  const { folderId } = useSelectedFolder();
  const { messageId } = useParams();

  const { data: message } = useMessage(messageId!);

  useEffect(() => {
    // Scroll to the top of the page
    window.scrollTo(0, 0);
  }, []);

  return message && folderId ? (
    folderId === 'draft' ? (
      <MessageEdit message={message} />
    ) : (
      <Message key={message.id} message={message} />
    )
  ) : null;
}
