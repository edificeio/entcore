import { useQuery } from '@tanstack/react-query';
import { mesEmpruntsQueryOptions } from '~/services/queries/mesEmprunts.queries';

export function useMesEmprunts() {
  const { data, isLoading, isError } = useQuery(mesEmpruntsQueryOptions);
  return { data, isLoading, isError };
}
