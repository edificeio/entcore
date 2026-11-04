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
  // `profiles` is not in the session `user` (userinfo) but in the description
  const { user, userDescription } = useUser();
  const queryClient = useQueryClient();
  const widgets = user?.widgets ?? [];
  const profiles = userDescription?.profiles ?? [];

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

  /**
   * Without a recorded preference a widget is shown or hidden according to
   * `defaultShow` (visible unless the order says otherwise for the profile).
   */
  const isVisible = (name: string, defaultShow = true) =>
    preferences?.[name]?.show ?? defaultShow;

  const isLocked = (name: string) =>
    widgets.find((widget) => widget.name === name)?.mandatory ?? false;

  const toggleWidget = (name: string, defaultShow = true) => {
    const current = preferences ?? {};
    // Flips the effective visibility, so the first click of a widget hidden
    // by default shows it
    const currentPref = current[name] ?? { index: 0, show: defaultShow };
    mutation.mutate({
      ...current,
      [name]: { ...currentPref, show: !currentPref.show },
    });
  };

  return { widgets, profiles, isLoading, isVisible, isLocked, toggleWidget };
}
