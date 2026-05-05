import { useTranslation } from 'react-i18next';
import './AppFooter.css';

export interface FooterLink {
  label: string;
  href: string;
}

export interface AppFooterProps {
  appName?: string;
  appDescription?: string;
  logoSrc?: string;
  usefulLinks?: FooterLink[];
  legalLinks?: FooterLink[];
}

const DEFAULT_USEFUL_LINKS: FooterLink[] = [
  { label: 'Signaler un problème', href: '#' },
  { label: 'Site de la Région', href: '#' },
];

const DEFAULT_LEGAL_LINKS: FooterLink[] = [
  { label: 'Mentions légales', href: '#' },
  { label: 'Politique de confidentialité', href: '#' },
  { label: "Charte d'utilisation", href: '#' },
  { label: 'Accessibilité', href: '#' },
];

export function AppFooter({
  appName,
  appDescription,
  logoSrc,
  usefulLinks = DEFAULT_USEFUL_LINKS,
  legalLinks = DEFAULT_LEGAL_LINKS,
}: AppFooterProps) {
  const { t } = useTranslation();

  return (
    <footer className="app-footer">
      <div className="app-footer-brand">
        <p className="app-footer-brand-name">
          {appName ?? t('homepage.footer.app-name', 'Lycée Connecté')}
        </p>
        <p className="app-footer-brand-description">
          {appDescription ??
            t(
              'homepage.footer.app-description',
              "L'environnement numérique de travail de la Région Nouvelle-Aquitaine",
            )}
        </p>
        {logoSrc && (
          <img
            src={logoSrc}
            alt={t('homepage.footer.logo-alt', 'Logo Région')}
            className="app-footer-brand-logo"
          />
        )}
      </div>

      <div className="app-footer-section">
        <p className="app-footer-section-title">
          {t('homepage.footer.useful-links', 'Liens utiles')}
        </p>
        <nav className="app-footer-links">
          {usefulLinks.map((link) => (
            <a key={link.href} href={link.href} className="app-footer-link">
              {link.label}
            </a>
          ))}
        </nav>
      </div>

      <div className="app-footer-section">
        <p className="app-footer-section-title">
          {t('homepage.footer.legal', 'Informations légales')}
        </p>
        <nav className="app-footer-links">
          {legalLinks.map((link) => (
            <a key={link.href} href={link.href} className="app-footer-link">
              {link.label}
            </a>
          ))}
        </nav>
      </div>
    </footer>
  );
}
