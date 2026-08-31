import { useQuery } from '@tanstack/react-query';
import { useMemo } from 'react';
import { groupEventsByDay } from '~/models/agenda';
import type { UseAgendaResult } from '~/models/agenda';
import { agendaQueryOptions } from '~/services/queries/agenda.queries';

export const useAgenda = (): UseAgendaResult => {
  const { data, isLoading, isError } = useQuery(agendaQueryOptions);

  const dayGroups = useMemo(() => groupEventsByDay(data ?? []), [data]);

  return { dayGroups, isLoading, isError };
};
