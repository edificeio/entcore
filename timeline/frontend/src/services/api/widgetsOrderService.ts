import { odeServices } from '@edifice.io/client';

export interface WidgetOrderEntry {
  /** Name of the widget in the platform catalog (`user.widgets`). */
  name: string;
  /** Whether the widget spans every column of the homepage grid. */
  fullWidth?: boolean;
  /**
   * Profiles (`Teacher`, `Student`, `Relative`, `Personnel`, `Guest`) for which
   * the widget is displayed until the user chooses otherwise. Absent: it is
   * displayed by default for everyone.
   */
  defaultVisibleFor?: string[];
}

/**
 * Per-project order of the homepage widgets, deployed with the platform
 * assets (`assets/widgets/widgets-order.json`) until a backend API exists.
 */
export const WIDGETS_ORDER_URL = '/assets/widgets/widgets-order.json';

const isEntry = (value: unknown): value is WidgetOrderEntry => {
  if (typeof value !== 'object' || value === null) return false;
  const { name, defaultVisibleFor } = value as WidgetOrderEntry;
  return (
    typeof name === 'string' &&
    (defaultVisibleFor === undefined ||
      (Array.isArray(defaultVisibleFor) &&
        defaultVisibleFor.every((profile) => typeof profile === 'string')))
  );
};

export const widgetsOrderService = {
  /**
   * Resolves `null` when the project provides no (valid) order, so that the
   * caller can fall back to its default one.
   */
  getOrder: async (): Promise<WidgetOrderEntry[] | null> => {
    try {
      const order: unknown = await odeServices.http().get(WIDGETS_ORDER_URL);
      if (odeServices.http().isResponseError()) return null;
      return Array.isArray(order) && order.every(isEntry) ? order : null;
    } catch {
      return null;
    }
  },
};
