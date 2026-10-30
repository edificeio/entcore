import {
  IconAgenda,
  IconCommunities,
  IconMaxicours,
  IconMediacentre,
  IconQwant,
  IconQwantJunior,
  IconUniversalis,
} from '@edifice.io/react/icons/apps';
import {
  IconBookmark,
  IconCalendar,
  IconGlobe,
  IconHome,
  IconLink,
  IconPlaceholder,
  IconTextPage,
  IconTrendingUp,
  IconWand,
} from '@edifice.io/react/icons';
import { ReactNode } from 'react';

/**
 * Best-effort icon per widget catalog name, pending real per-widget icons
 * from design (not covered by `IWidgetModel`, which has no icon field).
 * Applications with an icon in the EFF use it; the others get a generic one.
 * Falls back to a generic icon for anything not listed here.
 */
const WIDGET_ICONS: Record<string, ReactNode> = {
  'communities-widget': <IconCommunities />,
  'agenda-widget': <IconAgenda />,
  'calendar-widget': <IconCalendar />,
  'bookmark-widget': <IconLink />,
  'briefme-widget': <IconTextPage />,
  'qwant': <IconQwant />,
  'qwant-junior': <IconQwantJunior />,
  'universalis-widget': <IconUniversalis />,
  'cursus-widget': <IconTrendingUp />,
  'cantine-widget': <IconPlaceholder />,
  'edumalin-widget': <IconWand />,
  'rss-widget': <IconGlobe />,
  'carnet-de-bord': <IconBookmark />,
  'maxicours-widget': <IconMaxicours />,
  'mediacentre-widget': <IconMediacentre />,
};

export function getWidgetIcon(name: string): ReactNode {
  return WIDGET_ICONS[name] ?? <IconHome />;
}
