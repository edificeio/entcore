import { WidgetsPersonalizationPanel } from '@edifice.io/react/homepage';
import {
  IconBookmark,
  IconCalendarLight,
  IconClock,
  IconGlobe,
  IconLink,
  IconMegaphone,
  IconStar,
  IconTeacher,
} from '@edifice.io/react/icons';
import {
  IconAgenda,
  IconAlloregion,
  IconCommunities,
  IconEdt,
  IconMediacentre,
  IconNotes,
  IconQwant,
  IconUniversalis,
} from '@edifice.io/react/icons/apps';
import type { ReactNode } from 'react';
import { useTranslation } from 'react-i18next';
import { useAvailableWidgets } from '~/hooks/useAvailableWidgets';
import { useWidgetPreferences } from '~/hooks/useWidgetPreferences';
import type { WidgetId } from '~/models/widgetPreferences';

/** Listed in the same order as on the homepage. */
const WIDGETS: { id: WidgetId; label: string; icon: ReactNode }[] = [
  { id: 'communities', label: 'Communautés', icon: <IconCommunities /> },
  { id: 'mediacentre', label: 'Médiacentre', icon: <IconMediacentre /> },
  { id: 'avantages', label: 'Mes avantages', icon: <IconStar /> },
  { id: 'carnet-de-bord', label: 'Carnet de bord', icon: <IconNotes /> },
  { id: 'useful-links', label: 'Liens utiles', icon: <IconLink /> },
  { id: 'agenda', label: 'Agenda', icon: <IconAgenda /> },
  { id: 'timetable', label: 'Emploi du temps', icon: <IconEdt /> },
  { id: 'cantine', label: 'Menu de la cantine', icon: <IconCalendarLight /> },
  { id: 'screen-time', label: "Temps d'écran", icon: <IconClock /> },
  { id: 'edumalin', label: 'Édumalin', icon: <IconTeacher /> },
  { id: 'briefme', label: 'Brief.me', icon: <IconMegaphone /> },
  { id: 'rss', label: 'RSS', icon: <IconGlobe /> },
  { id: 'universalis', label: 'Universalis', icon: <IconUniversalis /> },
  { id: 'qwant', label: 'Qwant', icon: <IconQwant /> },
  { id: 'generation-hdf', label: 'Génération HDF', icon: <IconBookmark /> },
  { id: 'allo-region', label: 'Allô Région', icon: <IconAlloregion /> },
];

interface WidgetsSettingsPanelProps {
  onClose: () => void;
}

/** Lets the user show/hide the homepage widgets available to them. */
export function WidgetsSettingsPanel({ onClose }: WidgetsSettingsPanelProps) {
  const { t } = useTranslation('timeline');
  const available = useAvailableWidgets();
  const { isVisible, toggleWidget } = useWidgetPreferences();

  const items = WIDGETS.filter(({ id }) => available[id]).map(
    ({ id, label, icon }) => ({
      id,
      label: t(`homepage.crna.widget.settings.${id}`, label),
      icon,
      checked: isVisible(id),
    }),
  );

  return (
    <WidgetsPersonalizationPanel
      items={items}
      onToggle={(id) => void toggleWidget(id as WidgetId)}
      onClose={onClose}
    />
  );
}
