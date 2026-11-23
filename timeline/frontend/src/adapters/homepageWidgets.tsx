import {
  AgendaContainer,
  BriefMeContainer,
  CantineContainer,
  CarnetDeBordContainer,
  CommunitiesContainer,
  EdumalinContainer,
  GenerationHdfContainer,
  MediacentreContainer,
  QwantContainer,
  RssContainer,
  ScreenTimeContainer,
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
  /**
   * Workflow rights of the application behind the widget, one of which the
   * user needs to get it. `undefined` when it depends on no application right.
   */
  requiredWorkflows?: string[];
  Component: ComponentType;
}

const WIDGET_COMPONENTS: Record<string, ComponentType> = {
  'carnet-de-bord': CarnetDeBordContainer,
  'communities-widget': CommunitiesContainer,
  'agenda-widget': AgendaContainer,
  // "Liens utiles" is backed by the bookmark API
  'bookmark-widget': UsefulLinksContainer,
  'briefme-widget': BriefMeContainer,
  'qwant': QwantContainer,
  'universalis-widget': UniversalisContainer,
  'mediacentre-widget': MediacentreContainer,
  // Génération HDF is the `cursus-widget` of the platform catalog
  'cursus-widget': GenerationHdfContainer,
  'cantine-widget': CantineContainer,
  'edumalin-widget': EdumalinContainer,
  'rss-widget': RssContainer,
  'screen-time-widget': ScreenTimeContainer,
};

/**
 * The application right prevails over the widget one: an admin can grant (and
 * pin) a widget to users who cannot access its application, and the platform
 * catalog (`user.widgets`) keeps it when the widget is not linked to that
 * application. The component then renders nothing, so the widget must be left
 * out of the homepage and of the personalization panel. The rights are the
 * ones each component checks by itself.
 */
const WIDGET_REQUIRED_WORKFLOWS: Record<string, string[]> = {
  // Same rights as `CommunitiesContainer`
  'communities-widget': ['community.access', 'community.create'],
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
        requiredWorkflows: WIDGET_REQUIRED_WORKFLOWS[name],
        Component: WIDGET_COMPONENTS[name],
      },
    ];
  });
};
