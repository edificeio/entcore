import {
  AppHeader,
  Breadcrumb,
  Layout,
  LoadingScreen,
  useBreakpoint,
  useEdificeClient,
} from '@edifice.io/react';
import { QueryClient } from '@tanstack/react-query';
import { Suspense } from 'react';
import { Outlet, useLoaderData, useParams } from 'react-router-dom';
import { Config, existingActions } from '~/config';
import {
  CreateFolderModal,
  DesktopMenu,
  MobileMenu,
  MoveMessageToFolderModal,
  RenameFolderModal,
  SignatureModal,
  TrashFolderModal,
} from '~/features';
import { AppActionHeader } from '~/features/app/Action/AppActionHeader';
import {
  actionsQueryOptions,
  configQueryOptions,
  folderQueryOptions,
} from '~/services/queries';
import { setConfig, setWorkflows, useOpenedModal } from '~/store';
import MessageOnboardingModal from './components/MessageOnboardingModal';
import './index.css';

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

  const { lg } = useBreakpoint();
  const { messageId } = useParams();
  const isMessageDetail = !!messageId;
  const openedModal = useOpenedModal();

  if (!init || !currentApp) return <LoadingScreen position={false} />;

  if (!actions || !config) {
    throw 'Unexpected error';
  }

  return (
    <Layout>
      <div className="d-print-none">
        <AppHeader render={AppActionHeader}>
          <Breadcrumb app={currentApp!} />
        </AppHeader>
      </div>

      <Suspense fallback={<LoadingScreen />}>
        <MessageOnboardingModal />
      </Suspense>

      <div className="d-lg-flex">
        {!lg && !isMessageDetail && (
          <div className="d-print-none d-block d-lg-none px-0 py-12 border-bottom bg-white">
            <MobileMenu />
          </div>
        )}

        {lg && (
          <div className="d-print-none desktop-menu d-none d-lg-flex flex-column overflow-x-hidden p-16 ps-0 gap-16 border-end bg-white">
            <DesktopMenu />
          </div>
        )}

        <div className="align-self-lg-stretch flex-fill mx-n16 ms-lg-0 overflow-hidden">
          <Outlet />
        </div>
      </div>

      {openedModal === 'create' && <CreateFolderModal />}
      {openedModal === 'rename' && <RenameFolderModal />}
      {openedModal === 'trash' && <TrashFolderModal />}
      {openedModal === 'move-message' && <MoveMessageToFolderModal />}
      {openedModal === 'signature' && <SignatureModal />}
    </Layout>
  );
}
