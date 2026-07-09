import { queryOptions, useQuery } from '@tanstack/react-query';
import { fetchFlashMessageHistory } from '../api/flashMessage.api';

export const flashMessageHistoryQueryOptions = queryOptions({
  queryKey: ['flashMessage', 'history'],
  queryFn: fetchFlashMessageHistory,
  staleTime: 5 * 60 * 1000,
});

export const useFlashMessageHistory = () => useQuery(flashMessageHistoryQueryOptions);
