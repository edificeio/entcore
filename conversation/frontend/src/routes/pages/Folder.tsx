import { QueryClient } from '@tanstack/react-query';
import {
  LoaderFunctionArgs,
  useParams,
  useSearchParams,
} from 'react-router-dom';
import { MessageList } from '~/features/message-list/MessageList';
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
      const messagesQueryOptions = folderQueryOptions.getMessages(
        params.folderId,
        {
          search: search && search !== '' ? search : undefined,
          unread: unread === 'true' ? true : undefined,
        },
      );
      const messages =
        await queryClient.ensureInfiniteQueryData(messagesQueryOptions);
      return { messages };
    }
  };

export function Component() {
  const { folderId } = useParams();
  const [searchParams] = useSearchParams();
  const { messages, isPending: isLoadingMessage } = useFolderMessages(
    folderId!,
  );

  return (
    <>
      {(!!messages.length ||
        searchParams.get('search') ||
        searchParams.get('unread')) && <MessageListHeader />}
      <MessageList />
      {!isLoadingMessage && !messages.length && <MessageListEmpty />}
    </>
  );
}
