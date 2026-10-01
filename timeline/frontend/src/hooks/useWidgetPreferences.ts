import { odeServices, WidgetUserPref } from '@edifice.io/client';
import { useUser } from '@edifice.io/react';
import {
  queryOptions,
  useMutation,
  useQuery,
  useQueryClient,
} from '@tanstack/react-query';

export type WidgetPreferences = Record<string, WidgetUserPref>;

export const WIDGET_PREFERENCES_QUERY_KEY = ['widgets', 'preferences'];

function widgetPreferencesQueryOptions() {
  return queryOptions({
    queryKey: WIDGET_PREFERENCES_QUERY_KEY,
    queryFn: () => odeServices.widget().getPreferences(),
    staleTime: 15_000,
  });
}

/**
 * Wraps `odeServices.widget()` (the existing app-registry widget
 * preferences API — `user.widgets` for the deployed catalog + `mandatory`
 * for admin-pinned widgets, `getPreferences`/`setPreferences` for the
 * per-user show/hide flag) in a TanStack Query hook.
 */
export function useWidgetPreferences() {
  const { user } = useUser();
  const queryClient = useQueryClient();
  const widgets = user?.widgets ?? [];

  const { data: preferences, isLoading } = useQuery(
    widgetPreferencesQueryOptions(),
  );

  const mutation = useMutation({
    mutationFn: (prefs: WidgetPreferences) =>
      odeServices.widget().setPreferences(prefs),
    onMutate: async (prefs) => {
      await queryClient.cancelQueries({
        queryKey: WIDGET_PREFERENCES_QUERY_KEY,
      });
      queryClient.setQueryData(WIDGET_PREFERENCES_QUERY_KEY, prefs);
    },
    onError: () => {
      queryClient.invalidateQueries({
        queryKey: WIDGET_PREFERENCES_QUERY_KEY,
      });
    },
  });

  /** Widgets default to visible until the user explicitly hides them. */
  const isVisible = (name: string) => preferences?.[name]?.show ?? true;

  const isLocked = (name: string) =>
    widgets.find((widget) => widget.name === name)?.mandatory ?? false;

  const toggleWidget = (name: string) => {
    const current = preferences ?? {};
    const currentPref = current[name] ?? { index: 0, show: true };
    mutation.mutate({
      ...current,
      [name]: { ...currentPref, show: !currentPref.show },
    });
  };

  return { widgets, isLoading, isVisible, isLocked, toggleWidget };
}
