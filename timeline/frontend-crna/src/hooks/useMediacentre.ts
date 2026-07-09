import { useQuery } from '@tanstack/react-query';
import {
  mediacentrePinsQueryOptions,
  mediacentreQueryOptions,
  mediacentreUniversalisQueryOptions,
} from '~/services/queries/mediacentre.queries';

export function useMediacentre() {
  const { data, isLoading, isError } = useQuery(mediacentreQueryOptions);
  return { data, isLoading, isError };
}

export function useMediacentrePins(structureId: string | undefined) {
  return useQuery(mediacentrePinsQueryOptions(structureId ?? ''));
}

export function useMediacentreHasUniversalis() {
  const { data: hasUniversalis = false } = useQuery(
    mediacentreUniversalisQueryOptions,
  );
  return hasUniversalis;
}
