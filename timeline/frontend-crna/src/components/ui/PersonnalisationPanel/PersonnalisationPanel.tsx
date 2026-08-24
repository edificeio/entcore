import { ButtonBeta, Heading, IconButton, useOverlay } from '@edifice.io/react';
import { IconClose } from '@edifice.io/react/icons';
import { useTranslation } from 'react-i18next';
import { useFontPreference } from '~/hooks/useFontPreference';
import { useLanguagePreference } from '~/hooks/useLanguagePreference';
import './PersonnalisationPanel.css';

export function PersonnalisationPanel() {
  const { t } = useTranslation(['timeline', 'common']);
  const { updateOverlayOpen } = useOverlay();
  const closeOverlay = () => updateOverlayOpen(false);
  const { languages, currentLang, setLanguage } = useLanguagePreference();
  const { themes, currentTheme, setTheme } = useFontPreference();

  return (
    <div className="personnalisation-panel">
      <div className="personnalisation-panel-header">
        <Heading
          level="h2"
          headingStyle="h5"
          className="personnalisation-panel-title"
        >
          {t('homepage.crna.personnalisation.title', 'Personnalisation')}
        </Heading>
        <IconButton
          icon={<IconClose />}
          variant="ghost"
          color="tertiary"
          aria-label={t('close', 'Fermer')}
          onClick={closeOverlay}
        />
      </div>

      <section className="personnalisation-section">
        <Heading level="h3" headingStyle="h6">
          {t('homepage.crna.personnalisation.font', 'Police')}
        </Heading>
        <div className="personnalisation-toggle-group">
          {themes.map((theme) => (
            <ButtonBeta
              key={theme._id}
              variant="ghost"
              className={`personnalisation-toggle${theme._id === 'dyslexic' ? ' personnalisation-toggle--dyslexic' : ''}${currentTheme === theme._id ? ' personnalisation-toggle--active' : ''}`}
              onClick={() => setTheme(theme._id)}
            >
              {t(theme._id, { ns: 'common', defaultValue: theme.displayName })}
            </ButtonBeta>
          ))}
        </div>
      </section>

      <section className="personnalisation-section">
        <Heading level="h3" headingStyle="h6">
          {t('homepage.crna.personnalisation.language', "Langue de l'interface")}
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
