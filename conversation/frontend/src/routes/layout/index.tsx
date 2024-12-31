import { QueryClient } from '@tanstack/react-query';
import { Outlet, useLoaderData } from 'react-router-dom';
import { existingActions } from '~/config';
import { Folder } from '~/models';
//import { actions } from '~/config/actions';
import { actionsQueryOptions, folderQueryOptions } from '~/services/queries';

export const loader =
  (queryClient: QueryClient) =>
  async (/*{ params, request }: LoaderFunctionArgs*/) => {
    const foldersTreeOptions = folderQueryOptions.loadFoldersTree();
    const actionsOptions = actionsQueryOptions(existingActions);

    const [foldersTree, actions] = await Promise.all([
      queryClient.fetchQuery(foldersTreeOptions),
      queryClient.fetchQuery(actionsOptions),
    ]);

    return { foldersTree, actions };
  };

export function Component() {
  const { foldersTree, actions } = useLoaderData() as {
    foldersTree: Folder[];
    actions: Record<string, boolean>;
  };

  if (!foldersTree || !actions) {
    return null;
  }

  return (
    <div className="d-md-flex">
      <div className="d-block d-md-none p-12 border-bottom bg-white">
        TODO Combo
      </div>

      <div
        className="d-none d-md-flex flex-column overflow-x-hidden p-16 gap-16 border-end bg-white"
        style={{ width: '300px' }}
      >
        TODO {JSON.stringify(foldersTree)}
        <div className="w-100 border-bottom"></div>
        TODO Mes dossiers
        <div className="w-100 border-bottom"></div>
        TODO Espace utilisé
      </div>

      <div className="align-self-md-stretch">
        <Outlet />
      </div>
    </div>
  );
}
