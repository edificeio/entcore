import { useQuery } from '@tanstack/react-query';
import { avantagesQueryOptions } from '~/services/queries/avantages.queries';

export function useAvantages() {
  const { data, isLoading, isError } = useQuery(avantagesQueryOptions);
  return { data, isLoading, isError };
}
