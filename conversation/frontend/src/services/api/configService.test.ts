import { describe, expect } from 'vitest';
import { configService } from '~/services';
import { mockConfiguration, signaturePreferences } from '~/mocks';

describe('Conversation Configuration GET Methods', () => {
  it('should make a GET request to get global configuration', async () => {
    const response = await configService.getGlobalConfig();
    expect(response).toBeDefined();
    expect(response).toHaveProperty('max-depth');
    expect(response).toHaveProperty('recall-delay-minutes');
    expect(response).toStrictEqual(mockConfiguration);
  });

  it('should make a GET request to get signature preferences', async () => {
    const response = await configService.getSignaturePreferences();
    expect(response).toBeDefined();
    expect(response).toHaveProperty('useSignature');
    expect(response).toHaveProperty('signature');
    expect(response).toStrictEqual(signaturePreferences);
  });

  it('should make a PUT request to set signature preferences', async () => {
    const response = await configService.setSignaturePreferences({
      useSignature: false,
      signature: 'not used',
    });
    expect(response).toBeDefined();
  });
});
