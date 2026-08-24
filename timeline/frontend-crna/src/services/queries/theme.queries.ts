import { queryOptions } from '@tanstack/react-query';
import { fetchThemes } from '../api/theme.api';

export const themesQueryOptions = () =>
  queryOptions({
    queryKey: ['themes'],
    queryFn: fetchThemes,
    staleTime: Infinity,
  });
