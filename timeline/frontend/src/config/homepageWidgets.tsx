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
 * is displayed.
 */
export const resolveHomepageWidgets = (
  order: WidgetOrderEntry[],
): HomepageWidget[] =>
  order.flatMap(({ name, fullWidth }) => {
    const Component = WIDGET_COMPONENTS[name];
    return Component
      ? [{ name, fullWidth: fullWidth === true, Component }]
      : [];
  });
