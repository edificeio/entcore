import {
  HomepageWidget,
  resolveHomepageWidgets,
} from '~/adapters/homepageWidgets';
import { useWidgetPreferences } from './useWidgetPreferences';
import { useWidgetsOrder } from './useWidgetsOrder';

export interface UserHomepageWidget extends HomepageWidget {
  /** Whether it is displayed for the user until they choose otherwise. */
  defaultVisible: boolean;
}

/**
 * Widgets of the new homepage, in the display order of the project (see
 * `useWidgetsOrder`) — the same one applies to the homepage and to the
 * personalization panel, and users cannot change it.
 * - `available`: the widgets listed in the personalization panel (the ones
 *   deployed for the current user and given by the order, provided the user
 *   has the right of their application — it prevails over the widget right,
 *   even for a widget pinned by an admin).
 * - `displayed`: the widgets rendered on the homepage — `available` ones the
 *   user did not hide (a locked widget is always displayed). Without a choice
 *   of the user, a widget follows its `defaultVisible` (profile based).
 * - `isLoading`: the order is not known yet, nothing is listed meanwhile.
 */
export function useHomepageWidgets(): {
  available: UserHomepageWidget[];
  displayed: UserHomepageWidget[];
  isLoading: boolean;
} {
  const {
    widgets,
    profiles = [],
    workflows = [],
    isVisible,
    isLocked,
  } = useWidgetPreferences();
  const { order, isLoading } = useWidgetsOrder();
  const deployed = new Set<string>(widgets.map((widget) => widget.name));
  const hasApplicationRight = ({ requiredWorkflows }: HomepageWidget) =>
    !requiredWorkflows ||
    requiredWorkflows.some((workflow) => workflows.includes(workflow));

  const available = isLoading
    ? []
    : resolveHomepageWidgets(order)
        .filter(({ name }) => deployed.has(name))
        .filter(hasApplicationRight)
        .map((widget) => ({
          ...widget,
          defaultVisible:
            !widget.defaultVisibleFor ||
            widget.defaultVisibleFor.some((profile) =>
              (profiles as string[]).includes(profile),
            ),
        }));
  const displayed = available.filter(
    ({ name, defaultVisible }) =>
      isLocked(name) || isVisible(name, defaultVisible),
  );

  return { available, displayed, isLoading };
}
