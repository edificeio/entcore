import { queryOptions } from '@tanstack/react-query';
import { fetchAvantages } from '../api/avantages.api';

export const avantagesQueryOptions = queryOptions({
  queryKey: ['avantages'],
  queryFn: fetchAvantages,
  staleTime: 5 * 60 * 1000,
});
