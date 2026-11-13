import { beforeEach, describe, expect, it } from 'vitest';

import { resetModerationMocks } from '~/mocks/moderation/handlers';
import { moderationService } from '.';

describe('moderationService (mocked API)', () => {
  beforeEach(() => resetModerationMocks());

  it('searches cases to process, paginated', async () => {
    const result = await moderationService.searchCases({
      workflowState: 'PENDING',
    });

    expect(result.page).toBe(0);
    expect(result.items?.length).toBeLessThanOrEqual(result.size);
    expect(result.totalCount).toBeGreaterThan(0);
    result.items?.forEach((c) =>
      expect(['OPEN', 'PENDING', 'QUARANTINE']).toContain(c.status),
    );
  });

  it('gets a case by id', async () => {
    const { items } = await moderationService.searchCases({
      workflowState: 'PENDING',
    });
    const id = items![0].id;

    const found = await moderationService.getCase(id);

    expect(found.id).toBe(id);
    expect(found.reports.length).toBeGreaterThan(0);
  });

  it('moves a case to processed once a closing decision is posted', async () => {
    const { items } = await moderationService.searchCases({
      workflowState: 'PENDING',
    });
    const id = items![0].id;

    const { id: decisionId } = await moderationService.addDecision(id, {
      status: 'RESOURCE_KEPT',
      comment: 'pas un problème',
    });

    const updated = await moderationService.getCase(id);
    expect(updated.status).toBe('RESOURCE_KEPT');
    expect(updated.decisions?.at(-1)?.id).toBe(decisionId);
  });
});
