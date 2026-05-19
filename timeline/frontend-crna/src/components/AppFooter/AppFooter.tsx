import { Grid, Heading } from '@edifice.io/react';
import { useTranslation } from 'react-i18next';
import './AppFooter.css';
import logoCRNA from '~/assets/jeune-nouvelle-aquitaine.svg';

export interface FooterLink {
  label: string;
  href: string;
  isExternal?: boolean;
}

export interface AppFooterProps {
  appName?: string;
  appDescription?: string;
  usefulLinks?: FooterLink[];
  legalLinks?: FooterLink[];
}

const DEFAULT_USEFUL_LINKS: FooterLink[] = [
  { label: 'Site de la Région', href: 'https://www.nouvelle-aquitaine.fr/', isExternal: true },
  { label: 'Nous contacter', href: 'https://www.nouvelle-aquitaine.fr/contact', isExternal: true },
  { label: 'Plan du site', href: 'https://www.nouvelle-aquitaine.fr/plan-du-site', isExternal: true },
];

const DEFAULT_LEGAL_LINKS: FooterLink[] = [
  { label: 'Mentions légales', href: 'https://www.nouvelle-aquitaine.fr/mentions-legales', isExternal: true },
  { label: 'Données personnelles', href: 'https://www.nouvelle-aquitaine.fr/donnees-personnelles', isExternal: true },
  { label: 'Accessibilité : partiellement conforme', href: 'https://www.nouvelle-aquitaine.fr/accessibilite', isExternal: true },
];

export function AppFooter({
  appName,
  appDescription,
  usefulLinks = DEFAULT_USEFUL_LINKS,
  legalLinks = DEFAULT_LEGAL_LINKS,
}: AppFooterProps) {
  const { t } = useTranslation();

  return (
    <footer className="grid app-footer">
      <Grid.Col
        sm="4"
        lg="5"
        className="d-flex flex-column gap-8 app-footer-brand"
      >
        <a href="https://www.nouvelle-aquitaine.fr/" target="_blank" rel="noopener noreferrer">
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
        <img
          src={logoCRNA}
          alt={t('homepage.footer.logo-alt', 'Logo Région')}
          className="app-footer-brand-logo"
        />
        </a>
      </Grid.Col>

      <Grid.Col
        sm="2"
        lg="3"
        className="d-flex flex-column gap-4 app-footer-section"
      >
        <Heading
          level="h3"
          headingStyle="h5"
          className="app-footer-section-title"
        >
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

      <Grid.Col
        sm="2"
        lg="4"
        className="d-flex flex-column gap-4 app-footer-section"
      >
        <Heading
          level="h3"
          headingStyle="h5"
          className="app-footer-section-title"
        >
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
