import {
  queryOptions,
  useMutation,
  useQuery,
  useQueryClient,
} from '@tanstack/react-query';

import { type CaseSearchPayload, type DecisionPayload } from '~/models';
import { moderationService } from '../api';

export const moderationQueryKeys = {
  all: ['moderation'] as const,
  cases: () => [...moderationQueryKeys.all, 'cases'] as const,
  search: (payload: CaseSearchPayload, page: number) =>
    [...moderationQueryKeys.cases(), 'search', payload, page] as const,
  detail: (id: string) =>
    [...moderationQueryKeys.cases(), 'detail', id] as const,
};

export const moderationQueryOptions = {
  searchCases: (payload: CaseSearchPayload, page = 0) =>
    queryOptions({
      queryKey: moderationQueryKeys.search(payload, page),
      queryFn: () => moderationService.searchCases(payload, page),
    }),
  getCase: (id: string) =>
    queryOptions({
      queryKey: moderationQueryKeys.detail(id),
      queryFn: () => moderationService.getCase(id),
      enabled: !!id,
    }),
};

export const useSearchCases = (payload: CaseSearchPayload, page = 0) =>
  useQuery(moderationQueryOptions.searchCases(payload, page));

export const useCase = (id: string) =>
  useQuery(moderationQueryOptions.getCase(id));

/** Record a decision, then refresh every cached case (lists and detail). */
export const useAddDecision = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, payload }: { id: string; payload: DecisionPayload }) =>
      moderationService.addDecision(id, payload),
    onSuccess: () =>
      queryClient.invalidateQueries({ queryKey: moderationQueryKeys.cases() }),
  });
};
