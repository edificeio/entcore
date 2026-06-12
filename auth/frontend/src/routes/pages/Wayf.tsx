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

export const WayfPage = () => {
  const { t } = useTranslation('auth');
  const [childTheme, setChildTheme] = useState<string | undefined>();
  const [breadcrumb, setBreadcrumb] = useState<WayfParentProvider[]>([]);
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
    if (provider.children) {
      dirRef.current = 1;
      setBreadcrumb((prev) => [...prev, provider]);
    } else {
      window.location.href = provider.acs;
    }
  };

  const handleBack = () => {
    dirRef.current = -1;
    setBreadcrumb((prev) => prev.slice(0, -1));
  };

  const currentParent = breadcrumb[breadcrumb.length - 1];
  const transitionItem = {
    depth: breadcrumb.length,
    providers: currentParent ? currentParent.children : providers,
    parentIconKey: currentParent?.icon,
  };

  const transitions = useTransition(transitionItem, {
    keys: (item) => item.depth,
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
          <h1 className="wayf-title" data-testid="wayf-label-choice">{t('wayf.choice')}</h1>
          <div className="wayf-view-container">
            {transitions((style, item) => (
              <animated.div style={style} className="wayf-view-slide">
                <ProviderList
                  providers={item.providers}
                  onProviderClick={handleProviderClick}
                  parentIconKey={item.parentIconKey}
                  onBack={item.depth > 0 ? handleBack : undefined}
                />
              </animated.div>
            ))}
          </div>
        </div>

        {/* Pied de page */}
        <div className="wayf-selection__footer">
          <div className="wayf-footer-links">
            <a
              href={t('wayf.link.help.url')}
              className="wayf-help-btn"
              data-testid="wayf-link-help"
              target="_blank"
              rel="noreferrer"
            >
              {t('wayf.link.help.text') || "Besoin d'aide ?"}
            </a>
            <a
              href={t('auth.charter')}
              className="wayf-legal-link"
              data-testid="wayf-link-cgu"
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
            data-testid="wayf-link-edifice"
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
