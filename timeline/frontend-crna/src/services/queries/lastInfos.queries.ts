import { queryOptions } from '@tanstack/react-query';
import { fetchLastInfos } from '../api/lastInfos.api';

export const lastInfosQueryOptions = queryOptions({
  queryKey: ['last-infos'],
  queryFn: fetchLastInfos,
  staleTime: 5 * 60 * 1000,
});
