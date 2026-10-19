import { HOMEPAGE_WIDGETS, HomepageWidget } from '~/config/homepageWidgets';
import { useWidgetPreferences } from './useWidgetPreferences';

/**
 * Widgets of the new homepage, in the configured display order.
 * - `available`: the widgets listed in the personalization panel (the ones
 *   deployed for the current user).
 * - `displayed`: the widgets rendered on the homepage — `available` ones the
 *   user did not hide (a locked widget is always displayed).
 */
export function useHomepageWidgets(): {
  available: HomepageWidget[];
  displayed: HomepageWidget[];
} {
  const { widgets, isVisible, isLocked } = useWidgetPreferences();
  const deployed = new Set<string>(widgets.map((widget) => widget.name));

  const available = HOMEPAGE_WIDGETS.filter(({ name }) => deployed.has(name));
  const displayed = available.filter(
    ({ name }) => isLocked(name) || isVisible(name),
  );

  return { available, displayed };
}
