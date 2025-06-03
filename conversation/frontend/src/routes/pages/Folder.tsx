import { QueryClient } from '@tanstack/react-query';
import { Suspense } from 'react';
import {
  Await,
  LoaderFunctionArgs,
  useParams,
  useSearchParams,
} from 'react-router-dom';
import { MessageList } from '~/features/message-list/MessageList';
import { MessageListSkeleton } from '~/features/message-list/MessageListSkeleton';
import { MessageListEmpty } from '~/features/message-list/components/MessageListEmpty';
import { MessageListHeader } from '~/features/message-list/components/MessageListHeader';
import { folderQueryOptions, useFolderMessages } from '~/services';

export const loader =
  (queryClient: QueryClient) =>
  async ({ params, request }: LoaderFunctionArgs) => {
    const { searchParams } = new URL(request.url);
    const search = searchParams.get('search');
    const unread = searchParams.get('unread');
    if (params.folderId) {
      const messagesQuery = folderQueryOptions.getMessages(params.folderId, {
        search: search && search !== '' ? search : undefined,
        unread: unread === 'true' ? true : undefined,
      });
      queryClient.ensureInfiniteQueryData(messagesQuery);
    }
    return null;
  };

export function Component() {
  const { folderId } = useParams();
  const [searchParams] = useSearchParams();
  const { messages, isPending: isLoadingMessage } = useFolderMessages(
    folderId!,
  );
  const isLoadingMessagesFinishedPromise = new Promise((resolve) => {
    if (!isLoadingMessage && messages) {
      resolve(true);
    }
  });

  return (
    <Suspense fallback={<MessageListSkeleton />}>
      <Await resolve={isLoadingMessagesFinishedPromise}>
        {messages && (
          <>
            {(!!messages.length ||
              searchParams.get('search') ||
              searchParams.get('unread')) && <MessageListHeader />}
            <MessageList />
            {!isLoadingMessage && !messages.length && <MessageListEmpty />}
          </>
        )}
      </Await>
    </Suspense>
  );
}
