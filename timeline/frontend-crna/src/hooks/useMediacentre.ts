import { useQuery } from '@tanstack/react-query';
import { mediacentreQueryOptions } from '~/services/queries/mediacentre.queries';

export function useMediacentre() {
  const { data, isLoading, isError } = useQuery(mediacentreQueryOptions);
  return { data, isLoading, isError };
}
