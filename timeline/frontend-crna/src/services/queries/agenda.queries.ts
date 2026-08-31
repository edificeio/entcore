import { queryOptions } from '@tanstack/react-query';
import { fetchAgendaEvents } from '../api/agenda.api';

export const agendaQueryOptions = queryOptions({
  queryKey: ['agenda'],
  queryFn: fetchAgendaEvents,
  staleTime: 5 * 60 * 1000,
});
