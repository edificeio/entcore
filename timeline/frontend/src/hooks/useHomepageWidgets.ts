import {
  HomepageWidget,
  resolveHomepageWidgets,
} from '~/config/homepageWidgets';
import { useWidgetPreferences } from './useWidgetPreferences';
import { useWidgetsOrder } from './useWidgetsOrder';

/**
 * Widgets of the new homepage, in the display order of the project (see
 * `useWidgetsOrder`) — the same one applies to the homepage and to the
 * personalization panel, and users cannot change it.
 * - `available`: the widgets listed in the personalization panel (the ones
 *   deployed for the current user and given by the order).
 * - `displayed`: the widgets rendered on the homepage — `available` ones the
 *   user did not hide (a locked widget is always displayed).
 * - `isLoading`: the order is not known yet, nothing is listed meanwhile.
 */
export function useHomepageWidgets(): {
  available: HomepageWidget[];
  displayed: HomepageWidget[];
  isLoading: boolean;
} {
  const { widgets, isVisible, isLocked } = useWidgetPreferences();
  const { order, isLoading } = useWidgetsOrder();
  const deployed = new Set<string>(widgets.map((widget) => widget.name));

  const available = isLoading
    ? []
    : resolveHomepageWidgets(order).filter(({ name }) => deployed.has(name));
  const displayed = available.filter(
    ({ name }) => isLocked(name) || isVisible(name),
  );

  return { available, displayed, isLoading };
}
