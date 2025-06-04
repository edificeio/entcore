import {
  Layout,
  LoadingScreen,
  useEdificeClient,
  useEdificeTheme,
} from '@edifice.io/react';

import { matchPath } from 'react-router-dom';

import { basename } from '..';
import { useEffect } from 'react';
import { ApplicationList } from '~/features/application-list/ApplicationList';

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
  const { init } = useEdificeClient();
  const { theme } = useEdificeTheme();

  // Load theme
  useEffect(() => {
    if (!theme || !theme.themeUrl) return;
    const url = `${theme.themeUrl}theme.css`;
    const link = document.createElement('link');
    link.rel = 'stylesheet';
    link.href = url;
    document.head.appendChild(link);

    return () => {
      document.head.removeChild(link); // Nettoyage à la désactivation du composant
    };
  }, [theme]);

  if (!init) return <LoadingScreen position={false} />;

  return init ? (
    <Layout>
      <header>
        <h1>Mes applis</h1>
      </header>
      <ApplicationList />
    </Layout>
  ) : null;
};

export default Root;
