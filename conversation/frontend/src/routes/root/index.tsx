import { Layout, LoadingScreen, useEdificeClient } from '@edifice.io/react';

import { matchPath, Outlet } from 'react-router-dom';

import { basename } from '..';

function redirectTo(redirectPath: string) {
  window.location.replace(
    window.location.origin + basename.replace(/\/$/g, '') + redirectPath,
  );
}

/** Check old format URL and redirect if needed */
export const loader = async () => {
  const pathLocation = window.location.pathname;
  const hashLocation = window.location.hash.substring(1);

  if (
    pathLocation === '/conversation' ||
    pathLocation === '/conversation/conversation'
  ) {
    // Redirect to inbox
    redirectTo(`/id/inbox`);
    return;
  }

  // Check if the URL is an old format (angular root with hash) and redirect to the new format
  if (hashLocation) {
    const isFolder = matchPath('/:folderId', hashLocation);
    if (isFolder) {
      // Redirect to the new format
      const redirectPath = `/${isFolder.params.folderId}`;
      redirectTo(redirectPath);
      return;
    }

    const isReadMessage = matchPath('/read-mail/:mailId', hashLocation);
    if (isReadMessage) {
      // Redirect to the new format
      const redirectPath = `/id/inbox/${isReadMessage.params.mailId}`;
      redirectTo(redirectPath);
      return;
    }

    const isWriteMessage = matchPath('/write-mail', hashLocation);
    if (isWriteMessage) {
      // Redirect to the new format
      const redirectPath = `/id/draft`;
      redirectTo(redirectPath);
      return;
    }

    const isEditMessage = matchPath('/write-mail/:mailId', hashLocation);
    if (isEditMessage) {
      // Redirect to the new format
      const redirectPath = `/id/draft/${isEditMessage.params.mailId}`;
      redirectTo(redirectPath);
      return;
    }

    const isPrintMessage = matchPath('/printMail/:mailId', hashLocation);
    if (isPrintMessage) {
      // Redirect to the new format
      const redirectPath = `/print/${isPrintMessage.params.mailId}`;
      redirectTo(redirectPath);
      return;
    }
  }

  return null;
};

export const Root = () => {
  const { init, currentApp } = useEdificeClient();

  if (!init || !currentApp) return <LoadingScreen position={false} />;

  return (
    <Layout>
      <Outlet />
    </Layout>
  );
};

export default Root;
