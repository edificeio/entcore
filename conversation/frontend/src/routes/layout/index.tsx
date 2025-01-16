import {
  AppHeader,
  Breadcrumb,
  useBreakpoint,
  useEdificeClient,
} from '@edifice.io/react';
import { QueryClient } from '@tanstack/react-query';
import { Outlet, useLoaderData } from 'react-router-dom';
import { existingActions } from '~/config';
import { AppActionHeader } from '~/features/app/Action/AppActionHeader';
import { DesktopMenu, MobileMenu } from '~/features';
import { Folder } from '~/models';
import { actionsQueryOptions, folderQueryOptions } from '~/services/queries';
import './index.css';

export const loader =
  (queryClient: QueryClient) =>
  async (/*{ params, request }: LoaderFunctionArgs*/) => {
    const foldersTreeOptions = folderQueryOptions.getFoldersTree();
    const actionsOptions = actionsQueryOptions(existingActions);

    const [foldersTree, actions] = await Promise.all([
      queryClient.ensureQueryData(foldersTreeOptions),
      queryClient.ensureQueryData(actionsOptions),
    ]);

    return { foldersTree, actions };
  };

export function Component() {
  const { currentApp } = useEdificeClient();

  const { foldersTree, actions } = useLoaderData() as {
    foldersTree: Folder[];
    actions: Record<string, boolean>;
  };

  const { md } = useBreakpoint();

  if (!foldersTree || !actions) {
    return null;
  }

  return (
    <>
      <AppHeader render={AppActionHeader}>
        <Breadcrumb app={currentApp!} />
      </AppHeader>

      <div className="d-md-flex">
        <div className="d-block d-md-none px-0 py-12 border-bottom bg-white">
          {!md && <MobileMenu />}
        </div>

        <div className="desktop-menu d-none d-md-flex flex-column overflow-x-hidden p-16 ps-0 gap-16 border-end bg-white">
          {md && <DesktopMenu />}
        </div>

        <div className="align-self-md-stretch flex-fill mx-n16 ms-md-0">
          <Outlet />
        </div>
      </div>
    </>
  );
}
