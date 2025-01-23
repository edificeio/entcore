import { QueryClient } from '@tanstack/react-query';
import {
  LoaderFunctionArgs,
} from 'react-router-dom';
import { FolderHeader } from '~/features/Folder/folder-header';
import { FolderList } from '~/features/Folder/folder-list';
import {
  folderQueryOptions,
} from '~/services';

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
  return (
    <>
      <FolderHeader />
      <FolderList />
    </>
  );
}