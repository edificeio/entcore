import { QueryClient } from '@tanstack/react-query';
import {
  LoaderFunctionArgs,
  useParams,
  useSearchParams,
} from 'react-router-dom';
import { MessageList } from '~/features/Message/message-list';
import { MessageListEmpty } from '~/features/Message/message-list-empty';
import { MessageListHeader } from '~/features/Message/message-list-header';
import { folderQueryOptions, useFolderMessages } from '~/services';

export const loader =
  (queryClient: QueryClient) =>
  async ({ params, request }: LoaderFunctionArgs) => {
    const { searchParams } = new URL(request.url);
    const search = searchParams.get('search');
    const unread = searchParams.get('unread');
    const messagesQuery = folderQueryOptions.getMessages(params.folderId!, {
      search: search && search !== '' ? search : undefined,
      unread: unread === 'true' ? true : undefined,
    });
    const messages = await queryClient.ensureInfiniteQueryData(messagesQuery);
    return { messages };
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
