import {
  Layout,
  LoadingScreen,
  useEdificeClient,
  useEdificeTheme,
} from '@edifice.io/react';
import { matchPath } from 'react-router-dom';
import { basename } from '..';
import { useEffect, useState } from 'react';
import { MyAppLayout } from '~/layouts/MyAppsLayout';

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
  const [themeName, setThemeName] = useState('');
  // Load theme for icons
  useEffect(() => {
    if (!theme) return;
    const themeMode = theme.is1d ? 'one' : 'neo';
    const url = `/assets/themes/ode-bootstrap-${themeMode}/skins/default/theme.css`;
    const link = document.createElement('link');
    link.rel = 'stylesheet';
    link.href = url;
    document.head.appendChild(link);

    const themeName = theme.is1d ? '1d' : '2d';
    setThemeName(themeName);

    return () => {
      document.head.removeChild(link);
    };
  }, [theme]);

  if (!init) return <LoadingScreen position={false} />;

  return init ? (
    <Layout>
      <MyAppLayout theme={themeName} />
    </Layout>
  ) : null;
};

export default Root;
