import { queryOptions } from '@tanstack/react-query';
import { fetchHiddenWidgets } from '../api/widgetPreferences.api';

export const hiddenWidgetsQueryOptions = queryOptions({
  queryKey: ['widget-preferences', 'hidden'],
  queryFn: fetchHiddenWidgets,
  staleTime: 5 * 60 * 1000,
});
