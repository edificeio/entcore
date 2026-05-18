import { queryOptions } from '@tanstack/react-query';
import { fetchMesEmprunts } from '../api/mesEmprunts.api';

export const mesEmpruntsQueryOptions = queryOptions({
  queryKey: ['mes-emprunts'],
  queryFn: fetchMesEmprunts,
  staleTime: 5 * 60 * 1000,
});
