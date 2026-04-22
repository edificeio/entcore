import { useQuery } from '@tanstack/react-query';
import { lastInfosQueryOptions } from '~/services/queries/lastInfos.queries';

export function useLastInfos() {
  const { data, isLoading, isError } = useQuery(lastInfosQueryOptions);
  return { data, isLoading, isError };
}
