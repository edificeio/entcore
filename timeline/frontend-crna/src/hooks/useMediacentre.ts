import { useQuery } from '@tanstack/react-query';
import {
  mediacentreOrientationQueryOptions,
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

/** Only fetched once the orientation tab is opened, to avoid an extra request on every homepage load. */
export function useMediacentreOrientation(enabled: boolean) {
  return useQuery(mediacentreOrientationQueryOptions(enabled));
}

export function useMediacentreHasUniversalis() {
  const { data: hasUniversalis = false } = useQuery(
    mediacentreUniversalisQueryOptions,
  );
  return hasUniversalis;
}
