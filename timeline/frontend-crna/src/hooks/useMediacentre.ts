import { useQuery } from '@tanstack/react-query';
import { mediacentrePinsQueryOptions, mediacentreQueryOptions } from '~/services/queries/mediacentre.queries';

export function useMediacentre() {
  const { data, isLoading, isError } = useQuery(mediacentreQueryOptions);
  return { data, isLoading, isError };
}

export function useMediacentrePins(structureId: string | undefined) {
  return useQuery(mediacentrePinsQueryOptions(structureId ?? ''));
}
