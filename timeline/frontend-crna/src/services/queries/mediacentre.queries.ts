import { queryOptions } from '@tanstack/react-query';
import { fetchMediacentre, fetchMediacentrePins } from '../api/mediacentre.api';

export const mediacentreQueryOptions = queryOptions({
  queryKey: ['mediacentre'],
  queryFn: fetchMediacentre,
  staleTime: 5 * 60 * 1000,
});

export const mediacentrePinsQueryOptions = (structureId: string) =>
  queryOptions({
    queryKey: ['mediacentre', 'pins', structureId],
    queryFn: () => fetchMediacentrePins(structureId),
    staleTime: 5 * 60 * 1000,
    enabled: !!structureId,
  });
