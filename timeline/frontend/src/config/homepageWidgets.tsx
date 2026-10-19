import {
  AgendaContainer,
  BriefMeContainer,
  CantineContainer,
  CommunitiesContainer,
  GenerationHdfContainer,
  QwantContainer,
  RssContainer,
  UniversalisContainer,
  UsefulLinksContainer,
} from '@edifice.io/react/homepage';
import { ComponentType } from 'react';
import widgetsConfig from './widgets.json';

export interface HomepageWidget {
  /** Name of the widget in the platform catalog (`user.widgets`). */
  name: string;
  /** Whether the widget spans every column of the homepage grid. */
  fullWidth: boolean;
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
  'rss-widget': RssContainer,
};

/**
 * Widgets handled by the new homepage, in display order. The order is the
 * one of `widgets.json` — the same one applies to the homepage and to the
 * personalization panel, and users cannot change it.
 * TODO(IMPULS-6083): move this order to the platform/project configuration.
 */
export const HOMEPAGE_WIDGETS: HomepageWidget[] = widgetsConfig.map(
  (config) => ({
    name: config.name,
    fullWidth: 'fullWidth' in config && config.fullWidth === true,
    Component: WIDGET_COMPONENTS[config.name],
  }),
);
