import { useEffect, useState } from 'react';

interface Provider {
  name: string;
  uri: string;
}

export const App = () => {
  const [childTheme, setChildTheme] = useState<string | undefined>();
  const [providers, setProviders] = useState<Provider[] | undefined>();

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
        setProviders([
          { name: 'Provider 1', uri: '/saml/authn/provider1' },
          { name: 'Provider 2', uri: '/saml/authn/provider2' },
        ]);
      }
    }
  }, []);
  return (
    <div>
      <h1>WAYF v2</h1>
      {childTheme && <div>Child theme: {childTheme}</div>}
      {providers && (
        <div>
          <h2>Providers</h2>
          {providers.map((provider, index) => (
            <span key={provider.uri}>
              {provider.name}
              {index < providers.length - 1 ? ', ' : ''}
            </span>
          ))}
        </div>
      )}
      <div>
        <h2>Theme content</h2>
        {childTheme && (
          <img src={`/assets/themes/${childTheme}/img/logo.png`} alt="logo" />
        )}
      </div>
    </div>
  );
};
