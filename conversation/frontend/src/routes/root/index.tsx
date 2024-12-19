import {
  AppHeader,
  Breadcrumb,
  Layout,
  LoadingScreen,
  useOdeClient,
} from '@edifice-ui/react';

import { matchPath, Outlet } from 'react-router-dom';

import { basename } from '..';
import { AppActionBar } from '~/features/ActionBar/AppActionBar';

/** Check old format URL and redirect if needed */
export const loader = async () => {
  const hashLocation = location.hash.substring(1);

  // Check if the URL is an old format (angular root with hash) and redirect to the new format
  if (hashLocation) {
    const isPath = matchPath('/view/:id', hashLocation);

    if (isPath) {
      // Redirect to the new format
      const redirectPath = `/id/${isPath?.params.id}`;
      location.replace(
        location.origin + basename.replace(/\/$/g, '') + redirectPath,
      );
    }
  }

  return null;
};

export const Root = () => {
  const { init, currentApp } = useOdeClient();

  if (!init || !currentApp) return <LoadingScreen position={false} />;

  return (
    <Layout>
      <AppHeader render={() => <AppActionBar />}>
        <Breadcrumb app={currentApp} />
      </AppHeader>
      <Outlet />
    </Layout>
  );
};

export default Root;
