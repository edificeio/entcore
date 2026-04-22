import { ButtonBeta, Heading, IconButton, useOverlay } from '@edifice.io/react';
import { IconClose } from '@edifice.io/react/icons';
import { useTranslation } from 'react-i18next';
import { useLanguagePreference } from '~/hooks/useLanguagePreference';
// import { useThemePreference } from '~/hooks/useThemePreference';
import './PersonnalisationPanel.css';

const FONT_LABELS: Record<string, string | undefined> = {
  default: undefined,
  dyslexic: undefined,
};

export function PersonnalisationPanel() {
  const { t } = useTranslation();
  const { updateOverlayOpen } = useOverlay();
  const closeOverlay = () => updateOverlayOpen(false);
  // const { themes, currentSkin, setTheme } = useThemePreference();
  const { languages, currentLang, setLanguage } = useLanguagePreference();

  FONT_LABELS.default = t(
    'homepage.personnalisation.font.default',
    'Défaut',
  );
  FONT_LABELS.dyslexic = t(
    'homepage.personnalisation.font.dyslexic',
    'Dyslexique',
  );

  return (
    <div className="personnalisation-panel">
      <div className="personnalisation-panel-header">
        <Heading
          level="h2"
          headingStyle="h5"
          className="personnalisation-panel-title"
        >
          {t('homepage.personnalisation.title', 'Personnalisation')}
        </Heading>
        <IconButton
          icon={<IconClose />}
          variant="ghost"
          color="tertiary"
          aria-label={t('close', 'Fermer')}
          onClick={closeOverlay}
        />
      </div>

      {/* <section className="personnalisation-section">
        <Heading level="h3" headingStyle="h6">
          {t('homepage.personnalisation.font', 'Police')}
        </Heading>
        <div className="personnalisation-toggle-group">
          {themes.map((th) => (
            <ButtonBeta
              key={th._id}
              color={currentSkin === th._id ? 'destructive' : 'default'}
              variant={currentSkin === th._id ? 'filled' : 'outline'}
              className={`personnalisation-toggle${th._id === 'dyslexic' ? ' personnalisation-toggle--dyslexic' : ''}`}
              onClick={() => setTheme(th._id)}
            >
              {FONT_LABELS[th._id] ?? th.displayName}
            </ButtonBeta>
          ))}
        </div>
      </section> */}

      <section className="personnalisation-section">
        <Heading level="h3" headingStyle="h6">
          {t('homepage.personnalisation.language', "Langue de l'interface")}
        </Heading>
        <div className="personnalisation-lang-grid">
          {languages.map((l) => (
            <ButtonBeta
              key={l.code}
              variant="ghost"
              className={`personnalisation-lang-item${currentLang === l.code ? ' personnalisation-lang-item--active' : ''}`}
              onClick={() => setLanguage(l.code)}
            >
              <img
                className="personnalisation-lang-flag"
                src={`https://flagcdn.com/w80/${l.countryCode}.png`}
                alt={l.label}
              />
              <span className="personnalisation-lang-label">{l.label}</span>
            </ButtonBeta>
          ))}
        </div>
      </section>
    </div>
  );
}
