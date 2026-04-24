import { IconExternalLink } from '@edifice.io/react/icons';
import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import edificeLogoUrl from '~/assets/edifice-logo.svg';
import { ChildrenList } from '~/components/ChildrenList';
import { PartnerLogos } from '~/components/PartnerLogos';
import { ProviderList } from '~/components/ProviderList';
import { WelcomeMessage } from '~/components/WelcomeMessage';
import { useWayfConfig } from '~/hooks/useWayfConfig';
import type { WayfProvider } from '~/models/wayf';
import './Wayf.css';

type WayfView = { level: 1 } | { level: 2; parent: WayfProvider };

export const WayfPage = () => {
  const { t } = useTranslation('auth');
  const [childTheme, setChildTheme] = useState<string | undefined>();
  const [view, setView] = useState<WayfView>({ level: 1 });

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
    } else if (provider.children?.length) {
      setView({ level: 2, parent: provider });
    }
  };

  const handleChildClick = (child: WayfProvider) => {
    if (child.acs) {
      window.location.href = child.acs;
    }
  };

  const backgroundStyle = childTheme
    ? {
        backgroundImage: `url(/assets/themes/${childTheme}/img/background.png), var(--wayf-editorial-fallback)`,
      }
    : undefined;

  return (
    <div className="wayf-layout" style={backgroundStyle}>
      {/* Zone éditoriale — gauche */}
      <div className="wayf-editorial">
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
          {view.level === 2 ? (
            <ChildrenList
              parent={view.parent}
              onBack={() => setView({ level: 1 })}
              onChildClick={handleChildClick}
            />
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
