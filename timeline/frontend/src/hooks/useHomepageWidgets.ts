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
 * - `available`: the widgets listed in the personalization panel — every
 *   widget of the order that the homepage handles, whether or not the
 *   platform deploys it for the current user (`user.widgets` is ignored).
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
  const { profiles = [], isVisible, isLocked } = useWidgetPreferences();
  const { order, isLoading } = useWidgetsOrder();

  const available = isLoading
    ? []
    : resolveHomepageWidgets(order).map((widget) => ({
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
