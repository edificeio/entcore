import {
  AppHeader,
  Breadcrumb,
  Layout,
  LoadingScreen,
  Modal,
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
import { useAppActions, useOpenFolderModal } from '~/store';
import { Suspense } from 'react';
import { useTranslation } from 'react-i18next';

export const loader =
  (queryClient: QueryClient) =>
  async (/*{ params, request }: LoaderFunctionArgs*/) => {
    const foldersTreeOptions = folderQueryOptions.getFoldersTree();
    const actionsOptions = actionsQueryOptions(existingActions);

    try {
      const [foldersTree, actions] = await Promise.all([
        queryClient.ensureQueryData(foldersTreeOptions),
        queryClient.ensureQueryData(actionsOptions),
      ]);
      return { foldersTree, actions };
    } catch {
      return { foldersTree: [], actions: {} as Record<string, boolean> };
    }
  };

export function Component() {
  const { init, currentApp, appCode } = useEdificeClient();
  const { t } = useTranslation(appCode);

  const { foldersTree, actions } = useLoaderData() as {
    foldersTree: Folder[];
    actions: Record<string, boolean>;
  };

  const { md } = useBreakpoint();
  const { setFoldersTree, setOpenFolderModal } = useAppActions();
  const folderModal = useOpenFolderModal();

  const handleCloseFolderModal = () => setOpenFolderModal(null);

  if (!init || !currentApp) return <LoadingScreen position={false} />;

  if (!foldersTree || !actions) {
    throw 'Unexpected error';
  }

  // Update folders store
  setFoldersTree(foldersTree);

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

        <div className="align-self-md-stretch flex-fill mx-n16 ms-md-0">
          <Outlet />
        </div>
      </div>

      <Suspense>
        {folderModal === 'create' && (
          <Modal
            id="modalFolderCreate"
            isOpen={true}
            onModalClose={handleCloseFolderModal}
          >
            <Modal.Header onModalClose={handleCloseFolderModal}>
              {t('create.folder')}
            </Modal.Header>
            <Modal.Body>Tagada</Modal.Body>
            <Modal.Footer>Tsouin tsouin</Modal.Footer>
          </Modal>
        )}
      </Suspense>
    </Layout>
  );
}
