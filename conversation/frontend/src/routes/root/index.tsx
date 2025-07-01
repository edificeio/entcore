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
import { useLoaderData, useLocation, useParams } from 'react-router-dom';
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
import { ScrollableOutlet } from './components/ScrollableOutlet';
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

  const params = useParams();
  const location = useLocation();
  const isNewDraft = location.pathname === '/draft/create';
  const showMobileMenu = !(isNewDraft || params.messageId);

  const openedModal = useOpenedModal();

  if (!init || !currentApp) return <LoadingScreen position={false} />;

  if (!actions || !config) {
    throw 'Unexpected error';
  }

  return (
    <div className="d-print-block d-flex flex-column vh-100 flex-grow-1">
      <Layout>
        <div className="d-print-none">
          <AppHeader render={AppActionHeader}>
            <Breadcrumb app={currentApp!} />
          </AppHeader>
        </div>

        <Suspense fallback={<LoadingScreen />}>
          <MessageOnboardingModal />
        </Suspense>

        {!lg && (
          <div className="d-flex flex-column mx-n16 overflow-hidden ">
            {showMobileMenu && (
              <div className="d-print-none d-block px-0 py-12 border-bottom bg-white px-16">
                <MobileMenu />
              </div>
            )}

            <ScrollableOutlet />
          </div>
        )}

        {lg && (
          <div className="d-flex overflow-x-hidden flex-grow-1 me-n16 ">
            <div className="d-print-none desktop-menu flex-column p-16 ps-0 gap-16 border-end bg-white overflow-y-auto overflow-x-hidden">
              <DesktopMenu />
            </div>

            <ScrollableOutlet />
          </div>
        )}

        {(openedModal === 'create' || openedModal === 'create-then-move') && (
          <CreateFolderModal />
        )}
        {openedModal === 'rename' && <RenameFolderModal />}
        {openedModal === 'trash' && <TrashFolderModal />}
        {openedModal === 'move-message' && <MoveMessageToFolderModal />}
        {openedModal === 'signature' && <SignatureModal />}
      </Layout>
    </div>
  );
}
