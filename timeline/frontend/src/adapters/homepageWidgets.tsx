import {
  AgendaContainer,
  AlloRegionContainer,
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
  Stop3018Container,
  UniversalisContainer,
  UsefulLinksContainer,
} from '@edifice.io/react/homepage';
import { ComponentType } from 'react';
import {
  AgendaDemo,
  AlloRegionDemo,
  AvantagesDemo,
  BriefMeDemo,
  CantineDemo,
  CarnetDeBordDemo,
  CommunitiesDemo,
  EdumalinDemo,
  GenerationHdfDemo,
  MediacentreDemo,
  QwantDemo,
  RssDemo,
  ScreenTimeDemo,
  Stop3018Demo,
  TimetableDemo,
  UniversalisDemo,
  UsefulLinksDemo,
} from '~/demo/widgets';
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

/**
 * Demo mode (AO demo): the widgets render mocked data instead of their data
 * containers, so they can be shown on a platform without these services.
 * Set to `false` to go back to the containers.
 */
const DEMO_WIDGETS = true;

const CONTAINER_COMPONENTS: Record<string, ComponentType> = {
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
  // No platform catalog entry: shown to students and relatives only
  'stop3018-widget': Stop3018Container,
};

/** Same catalog names, rendered with mocked data (see `~/demo/widgets`). */
const DEMO_COMPONENTS: Record<string, ComponentType> = {
  'carnet-de-bord': CarnetDeBordDemo,
  'communities-widget': CommunitiesDemo,
  'agenda-widget': AgendaDemo,
  'bookmark-widget': UsefulLinksDemo,
  'briefme-widget': BriefMeDemo,
  'qwant': QwantDemo,
  'universalis-widget': UniversalisDemo,
  'mediacentre-widget': MediacentreDemo,
  'cursus-widget': GenerationHdfDemo,
  'cantine-widget': CantineDemo,
  'edumalin-widget': EdumalinDemo,
  'rss-widget': RssDemo,
  'screen-time-widget': ScreenTimeDemo,
  'stop3018-widget': Stop3018Demo,
  // Migrated from the CRNA homepage, demo only (no data container yet)
  'timetable-widget': TimetableDemo,
  'avantages-widget': AvantagesDemo,
};

const WIDGET_COMPONENTS = DEMO_WIDGETS ? DEMO_COMPONENTS : CONTAINER_COMPONENTS;

/**
 * "Allô Région" is not a catalog widget: its container shows itself from the
 * alloregion.access workflow. In demo mode it is always shown.
 */
export const AlloRegionWidget: ComponentType = DEMO_WIDGETS
  ? AlloRegionDemo
  : AlloRegionContainer;

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
