import {
  AgendaContainer,
  BriefMeContainer,
  CantineContainer,
  CommunitiesContainer,
  EdumalinContainer,
  GenerationHdfContainer,
  QwantContainer,
  RssContainer,
  UniversalisContainer,
  UsefulLinksContainer,
} from '@edifice.io/react/homepage';
import { ComponentType } from 'react';
import { WidgetOrderEntry } from '~/services';

export interface HomepageWidget {
  /** Name of the widget in the platform catalog (`user.widgets`). */
  name: string;
  /** Whether the widget spans every column of the homepage grid. */
  fullWidth: boolean;
  /** Profiles it is displayed for by default, `undefined` for everyone. */
  defaultVisibleFor?: string[];
  Component: ComponentType;
}

const WIDGET_COMPONENTS: Record<string, ComponentType> = {
  'communities-widget': CommunitiesContainer,
  'agenda-widget': AgendaContainer,
  // "Liens utiles" is backed by the bookmark API
  'bookmark-widget': UsefulLinksContainer,
  'briefme-widget': BriefMeContainer,
  'qwant': QwantContainer,
  'universalis-widget': UniversalisContainer,
  // Génération HDF is the `cursus-widget` of the platform catalog
  'cursus-widget': GenerationHdfContainer,
  'cantine-widget': CantineContainer,
  'edumalin-widget': EdumalinContainer,
  'rss-widget': RssContainer,
};

/**
 * Widgets of the given order that the new homepage handles, in that order.
 * Widgets absent from the order are left out: only what the project provides
 * is displayed. Unknown names and repeated entries are ignored (the first
 * one wins), the order comes from a deployed file that is not trusted.
 */
export const resolveHomepageWidgets = (
  order: WidgetOrderEntry[],
): HomepageWidget[] => {
  const seen = new Set<string>();
  return order.flatMap(({ name, fullWidth, defaultVisibleFor }) => {
    // Own keys only: "constructor" or "__proto__" must not resolve to a component
    const isHandled = Object.prototype.hasOwnProperty.call(
      WIDGET_COMPONENTS,
      name,
    );
    if (!isHandled || seen.has(name)) return [];
    seen.add(name);
    return [
      {
        name,
        fullWidth: fullWidth === true,
        defaultVisibleFor,
        Component: WIDGET_COMPONENTS[name],
      },
    ];
  });
};
