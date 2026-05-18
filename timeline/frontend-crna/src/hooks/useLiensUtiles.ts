import { useQuery } from '@tanstack/react-query';
import { liensUtilesQueryOptions } from '~/services/queries/liensUtiles.queries';

export function useLiensUtiles() {
  const { data, isLoading, isError } = useQuery(liensUtilesQueryOptions);
  return { data, isLoading, isError };
}
