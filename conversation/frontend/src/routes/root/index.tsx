import { Layout, LoadingScreen, useOdeClient } from '@edifice-ui/react';

import { matchPath, Outlet } from 'react-router-dom';

import { basename } from '..';

function redirectTo(redirectPath: string) {
  window.location.replace(
    window.location.origin + basename.replace(/\/$/g, '') + redirectPath,
  );
}

/** Check old format URL and redirect if needed */
export const loader = async () => {
  const hashLocation = window.location.hash.substring(1);

  // Check if the URL is an old format (angular root with hash) and redirect to the new format
  if (hashLocation) {
    const isFolder = matchPath('/:folderId', hashLocation);
    if (isFolder) {
      // Redirect to the new format
      const redirectPath = `/${isFolder.params.folderId}`;
      redirectTo(redirectPath);
      return;
    }

    const isMessage = matchPath('/read-mail/:mailId', hashLocation);
    if (isMessage) {
      // Redirect to the new format
      const redirectPath = `/inbox/${isMessage.params.mailId}`;
      redirectTo(redirectPath);
      return;
    }
  }

  return null;
};

export const Root = () => {
  const { init, currentApp } = useOdeClient();

  if (!init || !currentApp) return <LoadingScreen position={false} />;

  return (
    <Layout>
      <Outlet />
    </Layout>
  );
};

export default Root;
