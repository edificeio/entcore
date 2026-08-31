import {
  ButtonBeta,
  Heading,
  IconButton,
  Switch,
  useEdificeClient,
  useHasWorkflow,
  useOverlay,
} from '@edifice.io/react';
import {
  IconCalendar,
  IconClock,
  IconClose,
  IconLibrary,
  IconNotes,
  IconStar,
  IconUsers,
} from '@edifice.io/react/icons';
import type { ReactNode } from 'react';
import { useTranslation } from 'react-i18next';
import { useFontPreference } from '~/hooks/useFontPreference';
import { useLanguagePreference } from '~/hooks/useLanguagePreference';
import { useWidgetPreferences } from '~/hooks/useWidgetPreferences';
import type { WidgetId } from '~/models/widgetPreferences';
import './PersonnalisationPanel.css';

export function PersonnalisationPanel() {
  const { t } = useTranslation(['timeline', 'common']);
  const { updateOverlayOpen } = useOverlay();
  const closeOverlay = () => updateOverlayOpen(false);
  const { languages, currentLang, setLanguage } = useLanguagePreference();
  const { themes, currentTheme, setTheme } = useFontPreference();
  const { user } = useEdificeClient();
  const { isVisible, toggleWidget } = useWidgetPreferences();

  const hasWidget = (name: string) =>
    user?.widgets?.some((w) => (w.name as string) === name);
  // Mirrors Root.tsx's own hasMediacentreWidget check exactly.
  const hasMediacentreWorkflow = useHasWorkflow(
    'fr.openent.mediacentre.controller.MediacentreController|render',
  );

  const availableWidgets: Array<{
    id: WidgetId;
    label: string;
    icon: ReactNode;
  }> = [
    {
      id: 'communities',
      label: t('homepage.crna.widget.communities.title', 'Communautés'),
      icon: <IconUsers />,
    },
    ...(hasWidget('agenda-widget')
      ? [
          {
            id: 'agenda' as WidgetId,
            label: t('homepage.crna.widget.agenda.title', 'Agenda'),
            icon: <IconCalendar />,
          },
        ]
      : []),
    ...(hasWidget('carnet-de-bord')
      ? [
          {
            id: 'carnet-de-bord' as WidgetId,
            label: t(
              'homepage.crna.widget.carnet-de-bord.title',
              'Carnet de bord',
            ),
            icon: <IconNotes />,
          },
        ]
      : []),
    ...(hasWidget('mediacentre-widget') || hasMediacentreWorkflow
      ? [
          {
            id: 'mediacentre' as WidgetId,
            label: t('homepage.crna.widget.mediacentre.title', 'Médiacentre'),
            icon: <IconLibrary />,
          },
        ]
      : []),
    {
      id: 'avantages',
      label: t('homepage.crna.widget.avantages.title', 'Mes avantages'),
      icon: <IconStar />,
    },
    {
      id: 'timetable',
      label: t('homepage.crna.widget.timetable.title', 'Emploi du temps'),
      icon: <IconClock />,
    },
  ];

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
          {t(
            'homepage.crna.personnalisation.language',
            "Langue de l'interface",
          )}
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

      <section className="personnalisation-section">
        <Heading level="h3" headingStyle="h6">
          {t('homepage.crna.personnalisation.widgets', 'Widgets')}
        </Heading>
        <p className="personnalisation-widgets-description">
          {t(
            'homepage.crna.personnalisation.widgets.description',
            'Choisissez les widgets visibles sur votre page d’accueil',
          )}
        </p>
        <div className="personnalisation-widgets-list">
          {availableWidgets.map((widget) => (
            <div
              key={widget.id}
              className={`personnalisation-widget-item${isVisible(widget.id) ? ' personnalisation-widget-item--active' : ''}`}
            >
              <div className="personnalisation-widget-item-title">
                {widget.icon}
                <span>{widget.label}</span>
              </div>
              <Switch
                checked={isVisible(widget.id)}
                onChange={() => toggleWidget(widget.id)}
                aria-label={widget.label}
              />
            </div>
          ))}
        </div>
      </section>
    </div>
  );
}
