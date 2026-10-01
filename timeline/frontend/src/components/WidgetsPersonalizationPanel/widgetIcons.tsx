import {
  IconBookmark,
  IconCalendar,
  IconHome,
  IconLink,
} from '@edifice.io/react/icons';
import { ReactNode } from 'react';

/**
 * Best-effort icon per widget catalog name, pending real per-widget icons
 * from design (not covered by `IWidgetModel`, which has no icon field).
 * Falls back to a generic icon for anything not listed here.
 */
const WIDGET_ICONS: Record<string, ReactNode> = {
  'agenda-widget': <IconCalendar />,
  'calendar-widget': <IconCalendar />,
  'carnet-de-bord': <IconBookmark />,
  'universalis-widget': <IconBookmark />,
  'briefme-widget': <IconBookmark />,
  'qwant': <IconLink />,
  'qwant-junior': <IconLink />,
};

export function getWidgetIcon(name: string): ReactNode {
  return WIDGET_ICONS[name] ?? <IconHome />;
}
