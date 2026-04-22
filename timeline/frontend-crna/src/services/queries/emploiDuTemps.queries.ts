import { queryOptions } from '@tanstack/react-query';
import { fetchEmploiDuTemps } from '../api/emploiDuTemps.api';

export const emploiDuTempsQueryOptions = queryOptions({
  queryKey: ['emploi-du-temps'],
  queryFn: fetchEmploiDuTemps,
  staleTime: 5 * 60 * 1000,
});
