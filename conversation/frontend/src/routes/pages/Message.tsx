import { QueryClient } from '@tanstack/react-query';
import { Fragment, useEffect, useState } from 'react';
import {
  LoaderFunctionArgs,
  useParams,
  useSearchParams,
} from 'react-router-dom';
import { Message } from '~/features/message';
import { MessageEdit } from '~/features/message-edit/MessageEdit';
import {
  useAdditionalRecipients,
  useMessageReplyOrTransfer,
  useSelectedFolder,
} from '~/hooks';
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
  const { messageId: messageIdParam } = useParams();
  const { folderId } = useSelectedFolder();
  const [currentKey, setCurrentKey] = useState(0);
  const [searchParams] = useSearchParams();

  const { messageId, action } = useMessageIdAndAction(messageIdParam);
  const { message: templateMessage } = useMessageReplyOrTransfer({
    messageId,
    action,
  });
  const [message, setMessage] = useState(templateMessage);

  // Get IDs of users and groups/favorites to add as recipients.
  const toUsers = searchParams.getAll('user');
  const toGroups = searchParams.getAll('group');
  const toFavorites = searchParams.getAll('favorite');
  const { addRecipientsToMessage } = useAdditionalRecipients(
    'to',
    toUsers,
    toGroups,
    toFavorites,
  );
  useEffect(() => {
    if (templateMessage) {
      setMessage((msg) => ({
        ...msg,
        ...addRecipientsToMessage(templateMessage),
      }));
    }
  }, [templateMessage, addRecipientsToMessage]);

  useEffect(() => {
    // Scroll to the top of the page
    window.scrollTo(0, 0);
  }, []);

  useEffect(() => {
    // Update the current key to trigger a re-render
    setCurrentKey((prev) => prev + 1);
  }, [message]);

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
