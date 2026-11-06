import {
  infiniteQueryOptions,
  queryOptions,
  useInfiniteQuery,
  useMutation,
  useQuery,
  useQueryClient,
} from '@tanstack/react-query';

import { type DecisionPayload, type WorkflowState } from '~/models';
import { moderationService } from '../api';

export const moderationQueryKeys = {
  all: ['moderation'] as const,
  cases: () => [...moderationQueryKeys.all, 'cases'] as const,
  search: (workflowState: WorkflowState) =>
    [...moderationQueryKeys.cases(), 'search', workflowState] as const,
  detail: (id: string) =>
    [...moderationQueryKeys.cases(), 'detail', id] as const,
};

export const moderationQueryOptions = {
  /** Paginated search, one page fetched at a time (infinite loading). */
  searchCases: (workflowState: WorkflowState) =>
    infiniteQueryOptions({
      queryKey: moderationQueryKeys.search(workflowState),
      queryFn: ({ pageParam }) =>
        moderationService.searchCases({ workflowState }, pageParam),
      initialPageParam: 0,
      getNextPageParam: ({ page, size, totalCount }) =>
        (page + 1) * size < totalCount ? page + 1 : undefined,
    }),
  getCase: (id: string) =>
    queryOptions({
      queryKey: moderationQueryKeys.detail(id),
      queryFn: () => moderationService.getCase(id),
      enabled: !!id,
    }),
};

export const useSearchCases = (workflowState: WorkflowState) =>
  useInfiniteQuery(moderationQueryOptions.searchCases(workflowState));

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
