import { queryOptions } from '@tanstack/react-query';
import { fetchMediacentre } from '../api/mediacentre.api';

export const mediacentreQueryOptions = queryOptions({
  queryKey: ['mediacentre'],
  queryFn: fetchMediacentre,
  staleTime: 5 * 60 * 1000,
});
