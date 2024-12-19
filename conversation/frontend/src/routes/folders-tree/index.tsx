import { QueryClient } from '@tanstack/react-query';
import { existingActions } from '~/config';
//import { LoaderFunctionArgs } from 'react-router-dom';

//import { actions } from '~/config/actions';
import {
  actionsQueryOptions,
  foldersTreeQueryOptions,
} from '~/services/queries';

export const loader =
  (queryClient: QueryClient) =>
  async (/*{ params, request }: LoaderFunctionArgs*/) => {
    const foldersTree = foldersTreeQueryOptions();
    const actions = actionsQueryOptions(existingActions);

    await Promise.all([
      queryClient.fetchQuery(foldersTree),
      queryClient.fetchQuery(actions),
    ]);

    return null;
  };

export function Component() {
  return <></>;
}
