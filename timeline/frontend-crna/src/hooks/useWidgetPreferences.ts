import { useQuery, useQueryClient } from '@tanstack/react-query';
import { saveHiddenWidgets } from '~/services/api/widgetPreferences.api';
import type { WidgetId } from '~/models/widgetPreferences';
import { hiddenWidgetsQueryOptions } from '~/services/queries/widgetPreferences.queries';

export function useWidgetPreferences() {
  const queryClient = useQueryClient();
  const { data: hidden = [] } = useQuery(hiddenWidgetsQueryOptions);

  const isVisible = (id: WidgetId): boolean => !hidden.includes(id);

  const toggleWidget = async (id: WidgetId): Promise<void> => {
    const next = hidden.includes(id)
      ? hidden.filter((hiddenId) => hiddenId !== id)
      : [...hidden, id];
    queryClient.setQueryData(hiddenWidgetsQueryOptions.queryKey, next);
    await saveHiddenWidgets(next);
  };

  return { hidden, isVisible, toggleWidget };
}
