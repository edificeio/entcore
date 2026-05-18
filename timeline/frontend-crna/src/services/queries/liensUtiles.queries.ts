import { queryOptions } from '@tanstack/react-query';
import { fetchLiensUtiles } from '../api/liensUtiles.api';

export const liensUtilesQueryOptions = queryOptions({
  queryKey: ['liens-utiles'],
  queryFn: fetchLiensUtiles,
  staleTime: 5 * 60 * 1000,
});
