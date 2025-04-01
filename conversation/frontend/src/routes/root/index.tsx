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
import { Config, existingActions } from '~/config';
import { AppActionHeader } from '~/features/app/Action/AppActionHeader';
import {
  DesktopMenu,
  MobileMenu,
  TrashFolderModal,
  CreateFolderModal,
  RenameFolderModal,
  MoveMessageToFolderModal,
} from '~/features';
import {
  actionsQueryOptions,
  configQueryOptions,
  folderQueryOptions,
} from '~/services/queries';
import { setConfig, setWorkflows, useOpenFolderModal } from '~/store';
import './index.css';
import { AddMessageAttachmentToWorkspaceModal } from '~/features/modals/AddMessageAttachmentToWorkspaceModal';

// Typing for the root route loader.
export interface RootLoaderData {
  actions?: Record<string, boolean>;
  config?: Config;
}

export function loader(queryClient: QueryClient) {
  return async () => {
    try {
      const [actions, config] = await Promise.all([
        queryClient.ensureQueryData(actionsQueryOptions(existingActions)),
        queryClient.ensureQueryData(configQueryOptions.getGlobalConfig()),
      ]);

      // Store those constant values.
      if (actions) setWorkflows(actions);
      if (config) setConfig(config);

      // Ensure folders tree loads
      queryClient.ensureQueryData(
        folderQueryOptions.getFoldersTree(config.maxDepth),
      );
      return {
        actions,
        config,
      } as RootLoaderData;
    } catch {
      return {} as RootLoaderData;
    }
  };
}

export function Component() {
  const { init, currentApp } = useEdificeClient();

  const { actions, config } = useLoaderData() as RootLoaderData;

  const { md } = useBreakpoint();
  const folderModal = useOpenFolderModal();

  if (!init || !currentApp) return <LoadingScreen position={false} />;

  if (!actions || !config) {
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
      {folderModal === 'add-attachment-to-workspace' && (
        <AddMessageAttachmentToWorkspaceModal />
      )}
    </Layout>
  );
}
