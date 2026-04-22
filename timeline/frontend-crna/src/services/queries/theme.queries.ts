import { queryOptions } from '@tanstack/react-query';
import { fetchThemes, fetchCurrentTheme } from '../api/theme.api';

export const themesQueryOptions = () =>
  queryOptions({
    queryKey: ['themes'],
    queryFn: fetchThemes,
    staleTime: Infinity,
  });

export const currentThemeQueryOptions = () =>
  queryOptions({
    queryKey: ['theme', 'current'],
    queryFn: fetchCurrentTheme,
    staleTime: Infinity,
  });
