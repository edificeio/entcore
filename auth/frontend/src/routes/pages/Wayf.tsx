import { IconExternalLink } from '@edifice.io/react/icons';
import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import edificeLogoUrl from '~/assets/edifice-logo.svg';
import { Level2Stub } from '~/components/Level2Stub';
import { PartnerLogos } from '~/components/PartnerLogos';
import { ProviderList } from '~/components/ProviderList';
import { WelcomeMessage } from '~/components/WelcomeMessage';
import { useWayfConfig } from '~/hooks/useWayfConfig';
import type { WayfProvider } from '~/models/wayf';
import './Wayf.css';

export const WayfPage = () => {
  const { t } = useTranslation('auth');
  const [childTheme, setChildTheme] = useState<string | undefined>();
  const [selectedProvider, setSelectedProvider] = useState<WayfProvider | null>(
    null,
  );

  const { providers } = useWayfConfig();

  useEffect(() => {
    try {
      const content = document.getElementById('saml-wayf')?.textContent ?? '{}';
      const data = JSON.parse(content);
      if (data.childTheme) setChildTheme(data.childTheme);
    } catch {
      if (import.meta.env.DEV) {
        setChildTheme('theme-open-ent');
      }
    }
  }, []);

  const handleProviderClick = (provider: WayfProvider) => {
    if (provider.acs) {
      window.location.href = provider.acs;
    } else if (provider.children) {
      setSelectedProvider(provider);
    }
  };

  const backgroundStyle = childTheme
    ? {
        backgroundImage: `url(/assets/themes/${childTheme}/img/background.png)`,
      }
    : undefined;

  return (
    <div className="wayf-layout">
      {/* Zone éditoriale — gauche */}
      <div className="wayf-editorial" style={backgroundStyle}>
        <WelcomeMessage />
        <PartnerLogos />
      </div>

      {/* Zone de sélection — droite */}
      <div className="wayf-selection">
        {/* Logo projet */}
        <div className="wayf-selection__logo-bar">
          {childTheme && (
            <img
              className="wayf-logo"
              src={`/assets/themes/${childTheme}/img/logo.png`}
              alt="logo"
            />
          )}
        </div>

        {/* Espace authentification */}
        <div className="wayf-selection__auth">
          {selectedProvider ? (
            <Level2Stub onBack={() => setSelectedProvider(null)} />
          ) : (
            <>
              <h1 className="wayf-title">{t('wayf.choice')}</h1>
              <ProviderList
                providers={providers}
                onProviderClick={handleProviderClick}
              />
            </>
          )}

          <a href="#" className="wayf-help-btn">
            {t('wayf.link.help') || "Besoin d'aide ?"}
          </a>
        </div>

        {/* Pied de page */}
        <div className="wayf-selection__footer">
          <a
            href="#"
            className="wayf-legal-link"
            target="_blank"
            rel="noreferrer"
          >
            {t('wayf.link.cgu')}
            <IconExternalLink />
          </a>
          <a href="#" className="wayf-edifice-badge">
            <img
              src={edificeLogoUrl}
              alt="Édifice"
              className="wayf-edifice-badge__logo"
            />
          </a>
        </div>
      </div>
    </div>
  );
};
