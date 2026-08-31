import { useEdificeClient, useHasWorkflow } from '@edifice.io/react';
import type { WidgetId } from '~/models/widgetPreferences';

const MEDIACENTRE_RENDER_WORKFLOW =
  'fr.openent.mediacentre.controller.MediacentreController|render';
const COMMUNITY_WORKFLOWS = ['community.access', 'community.create'];

/**
 * Widgets the current user is allowed to display on the homepage (platform
 * widget or workflow right), regardless of their own show/hide preference.
 * Shared by the homepage and the widgets settings panel so both agree.
 */
export function useAvailableWidgets(): Record<WidgetId, boolean> {
  const { user } = useEdificeClient();
  const hasMediacentreRight =
    useHasWorkflow(MEDIACENTRE_RENDER_WORKFLOW) === true;
  const communityRights = useHasWorkflow(COMMUNITY_WORKFLOWS) as
    | Record<string, boolean>
    | undefined;

  const hasWidget = (name: string) =>
    user?.widgets?.some((widget) => (widget.name as string) === name) ?? false;

  return {
    'communities': COMMUNITY_WORKFLOWS.some(
      (workflow) => communityRights?.[workflow] === true,
    ),
    'mediacentre': hasWidget('mediacentre-widget') || hasMediacentreRight,
    'avantages': true,
    'carnet-de-bord': hasWidget('carnet-de-bord'),
    // Shown to everyone on purpose, whatever the bookmark-widget/alloregion rights.
    'useful-links': true,
    'agenda': hasWidget('agenda-widget'),
    'timetable': true,
    'cantine': true,
    'screen-time': true,
    'edumalin': true,
    'briefme': true,
    'rss': true,
    'universalis': true,
    'qwant': true,
    'generation-hdf': true,
    'allo-region': true,
  };
}
