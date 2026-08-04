import { describe, expect, test } from 'vitest';
import { absenceService } from '.';
import { mockAbsenceSettings } from '~/mocks';

describe('Conversation Absence GET Methods', () => {
  test('makes a GET request to get the current absence settings', async () => {
    const response = await absenceService.getSettings();

    expect(response).toBeDefined();
    expect(response).toStrictEqual(mockAbsenceSettings);
  });
});

describe('Conversation Absence Mutation Methods', () => {
  test('makes a PUT request to save the absence settings', async () => {
    const response = await absenceService.saveSettings({
      enabled: false,
      startAt: mockAbsenceSettings.startAt,
      endAt: mockAbsenceSettings.endAt,
      bodyJson: mockAbsenceSettings.bodyJson,
    });

    expect(response).toBeDefined();
    expect(response.enabled).toBe(false);
  });
});
