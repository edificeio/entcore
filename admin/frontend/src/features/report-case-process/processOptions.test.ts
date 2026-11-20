import { describe, expect, it } from 'vitest';

import { type Status } from '~/models';
import { PROCESS_OPTIONS, canProcess, processOptions } from './processOptions';

const statuses = (status: Status) =>
  processOptions(status).map((option) => option.status);

describe('processOptions', () => {
  it('offers keep and delete on a pending case', () => {
    expect(statuses('OPEN')).toEqual(['RESOURCE_KEPT', 'RESOURCE_DELETED']);
    expect(statuses('PENDING')).toEqual(['RESOURCE_KEPT', 'RESOURCE_DELETED']);
    expect(statuses('QUARANTINE')).toEqual([
      'RESOURCE_KEPT',
      'RESOURCE_DELETED',
    ]);
  });

  it('only offers delete once the content is kept', () => {
    expect(statuses('RESOURCE_KEPT')).toEqual(['RESOURCE_DELETED']);
  });

  it('offers nothing once the content is deleted', () => {
    expect(statuses('RESOURCE_DELETED')).toEqual([]);
    expect(canProcess('RESOURCE_DELETED')).toBe(false);
    expect(canProcess('RESOURCE_KEPT')).toBe(true);
    expect(canProcess('OPEN')).toBe(true);
  });

  it('keeps quarantine configured but hidden', () => {
    expect(
      PROCESS_OPTIONS.find((option) => option.status === 'QUARANTINE'),
    ).toMatchObject({ enabled: false, tab: 'PENDING' });
  });
});
