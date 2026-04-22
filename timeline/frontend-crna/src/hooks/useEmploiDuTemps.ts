import { useQuery } from '@tanstack/react-query';
import { emploiDuTempsQueryOptions } from '~/services/queries/emploiDuTemps.queries';

export function useEmploiDuTemps() {
  const { data, isLoading, isError } = useQuery(emploiDuTempsQueryOptions);
  return { data, isLoading, isError };
}
