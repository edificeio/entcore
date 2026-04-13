import { useEffect, useState } from 'react';

export const App = () => {
  const [childTheme, setChildTheme] = useState<string | undefined>();
  const [providers, setProviders] = useState<string[] | undefined>();

  useEffect(() => {
    try {
      const content = document.getElementById('saml-wayf')?.textContent || '{}';
      const data = JSON.parse(content);
      setChildTheme(data.childTheme);
      setProviders(data.providers);
    } catch (error) {
      console.error('Failed to parse SAML WAYF data:', error);
      // FIXME: Find a better way to handle this case
      const isDev = import.meta.env.DEV;
      // Use default values for development
      if (isDev) {
        setChildTheme('theme-open-ent');
        setProviders(['Provider 1', 'Provider 2']);
      }
    }
  }, []);
  return (
    <div>
      <h1>WAYF v2</h1>
      {childTheme && <div>Child theme: {childTheme}</div>}
      {providers && <div>Providers: {providers.join(', ')}</div>}
      <div>
        <h2>Theme content</h2>
        <img src={`/assets/themes/${childTheme}/img/logo.png`} alt="logo" />
      </div>
    </div>
  );
};
