import { QueryClient } from '@tanstack/react-query';
import { LoaderFunctionArgs } from 'react-router-dom';

//import { actions } from '~/config/actions';
import { availableActionsQuery, foldersTreeQuery } from '~/services/queries';

export const loader =
  (queryClient: QueryClient) =>
  async ({ params, request }: LoaderFunctionArgs) => {
    const queryFoldersTree = foldersTreeQuery();
    const actions = availableActionsQuery();

    await Promise.all([
      queryClient.fetchInfiniteQuery(queryPostsList),
      queryClient.fetchQuery(queryBlogCounter),
      queryClient.fetchQuery(actions),
    ]);

    return null;
  };

export function Component() {
  return <Blog />;
}
