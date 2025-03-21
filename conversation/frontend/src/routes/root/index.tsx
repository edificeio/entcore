import {
  AppHeader,
  Breadcrumb,
  Layout,
  LoadingScreen,
  useBreakpoint,
  useEdificeClient,
} from '@edifice.io/react';
import { QueryClient } from '@tanstack/react-query';
import { Outlet, useLoaderData } from 'react-router-dom';
import { existingActions } from '~/config';
import { AppActionHeader } from '~/features/app/Action/AppActionHeader';
import {
  DesktopMenu,
  MobileMenu,
  TrashFolderModal,
  CreateFolderModal,
  RenameFolderModal,
  MoveMessageToFolderModal,
} from '~/features';
import { actionsQueryOptions, folderQueryOptions } from '~/services/queries';
import { useOpenFolderModal } from '~/store';
import './index.css';

export const loader = (queryClient: QueryClient) => async () => {
  const actionsOptions = actionsQueryOptions(existingActions);

  // Non-blocking: display a skeleton in the meantime
  queryClient.ensureQueryData(folderQueryOptions.getFoldersTree());

  try {
    const actions = await queryClient.ensureQueryData(actionsOptions);
    return { actions };
  } catch {
    return { actions: {} as Record<string, boolean> };
  }
};

export function Component() {
  const { init, currentApp } = useEdificeClient();

  const { actions } = useLoaderData() as {
    actions: Record<string, boolean>;
  };

  const { md } = useBreakpoint();
  const folderModal = useOpenFolderModal();

  if (!init || !currentApp) return <LoadingScreen position={false} />;

  if (!actions) {
    throw 'Unexpected error';
  }

  return (
    <Layout>
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

        <div className="align-self-md-stretch flex-fill mx-n16 ms-md-0 overflow-hidden">
          <Outlet />
        </div>
      </div>

      {folderModal === 'create' && <CreateFolderModal />}
      {folderModal === 'rename' && <RenameFolderModal />}
      {folderModal === 'trash' && <TrashFolderModal />}
      {folderModal === 'move-message' && <MoveMessageToFolderModal />}
    </Layout>
  );
}
