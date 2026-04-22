import { queryOptions } from '@tanstack/react-query';
import { fetchLanguages } from '../api/language.api';

export const languagesQueryOptions = () =>
  queryOptions({
    queryKey: ['languages'],
    queryFn: fetchLanguages,
    staleTime: Infinity,
  });
