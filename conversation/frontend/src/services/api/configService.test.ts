import { describe, expect, test } from 'vitest';
import { configService } from '~/services';
import { mockConfiguration } from '~/mocks';

describe('Conversation Configuration GET Methods', () => {
  test('makes a GET request to get global configuration', async () => {
    const response = await configService.getGlobalConfig();

    expect(response).toBeDefined();
    expect(response).toHaveProperty('max-depth');
    expect(response).toHaveProperty('recall-delay-minutes');
    expect(response).toStrictEqual(mockConfiguration);
  });
});
