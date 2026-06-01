import { IconExternalLink } from '@edifice.io/react/icons';
import { animated, useTransition } from '@react-spring/web';
import { useEffect, useRef, useState } from 'react';
import { useTranslation } from 'react-i18next';
import edificeLogoUrl from '~/assets/edifice-logo.svg';
import { PartnerLogos } from '~/components/PartnerLogos';
import { ProviderList } from '~/components/ProviderList';
import { WelcomeMessage } from '~/components/WelcomeMessage';
import { useWayfConfig } from '~/hooks/useWayfConfig';
import { useWelcomeMessage } from '~/hooks/useWelcomeMessage';
import type { WayfParentProvider, WayfProvider } from '~/models/wayf';
import './Wayf.css';

type WayfView = { level: 1 } | { level: 2; parent: WayfParentProvider };

export const WayfPage = () => {
  const { t } = useTranslation('auth');
  const [childTheme, setChildTheme] = useState<string | undefined>();
  const [view, setView] = useState<WayfView>({ level: 1 });
  const dirRef = useRef<1 | -1>(1);

  const { providers, partners } = useWayfConfig();
  const welcomeState = useWelcomeMessage();

  useEffect(() => {
    let theme = '';
    try {
      const content = document.getElementById('saml-wayf')?.textContent ?? '{}';
      const data = JSON.parse(content);
      if (typeof data.childTheme === 'string') theme = data.childTheme;
    } catch {
      // Malformed JSON — keep theme empty and fall back below.
    }
    // In dev the backend isn't there, so the Mustache placeholder is left as-is.
    if (theme && !theme.includes('{{')) {
      setChildTheme(theme);
    } else if (import.meta.env.DEV) {
      setChildTheme('theme-open-ent');
    }
  }, []);

  const handleProviderClick = (provider: WayfProvider) => {
    if ('acs' in provider) {
      window.location.href = provider.acs;
    } else {
      dirRef.current = 1;
      setView({ level: 2, parent: provider });
    }
  };

  const handleBack = () => {
    dirRef.current = -1;
    setView({ level: 1 });
  };

  const transitions = useTransition(view, {
    from: () => ({
      transform: `translateX(${dirRef.current * 100}%)`,
      opacity: 0,
    }),
    enter: { transform: 'translateX(0%)', opacity: 1 },
    leave: () => ({
      transform: `translateX(${dirRef.current * -100}%)`,
      opacity: 0,
    }),
    config: { tension: 280, friction: 28 },
  });

  const backgroundStyle = childTheme
    ? {
        backgroundImage: `url(/assets/themes/${childTheme}/img/background.png), var(--wayf-editorial-fallback)`,
      }
    : undefined;

  return (
    <div className="wayf-layout" style={backgroundStyle}>
      {/* Zone éditoriale — gauche */}
      <div className="wayf-editorial">
        <WelcomeMessage state={welcomeState} />
        <PartnerLogos partners={partners} />
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
          <h1 className="wayf-title">{t('wayf.choice')}</h1>
          <div className="wayf-view-container">
            {transitions((style, v) => (
              <animated.div style={style} className="wayf-view-slide">
                {v.level === 2 ? (
                  <ProviderList
                    providers={v.parent.children}
                    onProviderClick={handleProviderClick}
                    parentIconKey={v.parent.icon}
                    onBack={handleBack}
                  />
                ) : (
                  <ProviderList
                    providers={providers}
                    onProviderClick={handleProviderClick}
                  />
                )}
              </animated.div>
            ))}
          </div>
        </div>

        {/* Pied de page */}
        <div className="wayf-selection__footer">
          <div className="wayf-footer-links">
            <a href={t('wayf.link.help.url')} className="wayf-help-btn">
              {t('wayf.link.help.text') || "Besoin d'aide ?"}
            </a>
            <a
              href={t('auth.charter')}
              className="wayf-legal-link"
              target="_blank"
              rel="noreferrer"
            >
              {t('wayf.link.cgu')}
              <IconExternalLink />
            </a>
          </div>
          <a
            href="https://edifice.io/"
            target="_blank"
            className="wayf-edifice-badge"
          >
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
