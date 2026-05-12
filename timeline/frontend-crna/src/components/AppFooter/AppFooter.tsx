import { Grid, Heading } from '@edifice.io/react';
import { useTranslation } from 'react-i18next';
import './AppFooter.css';

export interface FooterLink {
  label: string;
  href: string;
  isExternal?: boolean;
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
    <footer className="grid app-footer">
      <Grid.Col sm="4" lg="5" className="d-flex flex-column gap-8 app-footer-brand">
        <Heading level="h2" headingStyle="h4" className="app-footer-brand-name">
          {appName ?? t('homepage.footer.app-name', 'Lycée Connecté')}
        </Heading>
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
      </Grid.Col>

      <Grid.Col sm="2" lg="3" className="d-flex flex-column gap-4 app-footer-section">
        <Heading level="h3" headingStyle="h5" className="app-footer-section-title">
          {t('homepage.footer.useful-links', 'Liens utiles')}
        </Heading>
        <nav className="d-flex flex-column gap-4 app-footer-links">
          {usefulLinks.map((link) => (
            <a
              key={link.href}
              href={link.href}
              className="app-footer-link"
              target={link.isExternal ? '_blank' : undefined}
              rel={link.isExternal ? 'noopener noreferrer' : undefined}
            >
              {link.label}
            </a>
          ))}
        </nav>
      </Grid.Col>

      <Grid.Col sm="2" lg="4" className="d-flex flex-column gap-4 app-footer-section">
        <Heading level="h3" headingStyle="h5" className="app-footer-section-title">
          {t('homepage.footer.legal', 'Informations légales')}
        </Heading>
        <nav className="d-flex flex-column gap-4 app-footer-links">
          {legalLinks.map((link) => (
            <a
              key={link.href}
              href={link.href}
              className="app-footer-link"
              target={link.isExternal ? '_blank' : undefined}
              rel={link.isExternal ? 'noopener noreferrer' : undefined}
            >
              {link.label}
            </a>
          ))}
        </nav>
      </Grid.Col>
    </footer>
  );
}
