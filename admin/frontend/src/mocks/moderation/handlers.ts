import { HttpResponse, http } from 'msw';

import {
  type Case,
  type CaseSearch,
  type CaseSearchPayload,
  type DecisionPayload,
  type Status,
  type WorkflowState,
} from '~/models';
import { moderationBaseUrl } from '~/services/api';
import { createMockCases } from './data';

/** Mock page size, until the back-end confirms it. */
const PAGE_SIZE = 20;

/**
 * Assumed mapping between workflow states and case statuses.
 * To be confirmed by the back-end team.
 */
const STATUSES_BY_WORKFLOW_STATE: Record<WorkflowState, Status[]> = {
  PENDING: ['OPEN', 'PENDING', 'QUARANTINE'],
  SETTLE: ['RESOURCE_DELETED', 'RESOURCE_KEPT', 'CLOSED'],
};

let cases: Case[] = createMockCases();

/** Restore the initial data set (useful between tests). */
export const resetModerationMocks = () => {
  cases = createMockCases();
};

export const moderationHandlers = [
  http.get(`${moderationBaseUrl}/cases/:id`, ({ params }) => {
    const found = cases.find((c) => c.id === params.id);
    return found
      ? HttpResponse.json(found)
      : HttpResponse.json({ error: 'case.not.found' }, { status: 404 });
  }),

  http.post(`${moderationBaseUrl}/cases/_search`, async ({ request }) => {
    const { workflowState } = (await request.json()) as CaseSearchPayload;
    const statuses = STATUSES_BY_WORKFLOW_STATE[workflowState];
    if (!statuses) {
      return HttpResponse.json(
        { error: 'invalid.workflowState' },
        { status: 400 },
      );
    }

    const page = Number(new URL(request.url).searchParams.get('page') ?? 0);
    const matching = cases.filter((c) => statuses.includes(c.status));

    return HttpResponse.json<CaseSearch>({
      totalCount: matching.length,
      page,
      size: PAGE_SIZE,
      items: matching.slice(page * PAGE_SIZE, (page + 1) * PAGE_SIZE),
    });
  }),

  http.post(
    `${moderationBaseUrl}/cases/:id/decisions`,
    async ({ params, request }) => {
      const target = cases.find((c) => c.id === params.id);
      if (!target) {
        return HttpResponse.json({ error: 'case.not.found' }, { status: 404 });
      }

      const { status, comment } = (await request.json()) as DecisionPayload;
      const decisionId = crypto.randomUUID();
      target.status = status;
      target.modifiedAt = new Date().toISOString();
      target.decisions = [
        ...(target.decisions ?? []),
        {
          id: decisionId,
          userId: 'a1b2c3d4',
          displayName: 'Fake Admin',
          status,
          comment,
        },
      ];

      return HttpResponse.json({ id: decisionId });
    },
  ),
];
