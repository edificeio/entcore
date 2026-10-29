import i18next from 'i18next';
import { readFileSync } from 'node:fs';
import { resolve } from 'node:path';

const frTranslations = JSON.parse(
  readFileSync(
    resolve(
      __dirname,
      '../../../../../backend/src/main/resources/i18n/fr.json',
    ),
    'utf-8',
  ),
);

describe('Dragged messages label', () => {
  it('is pluralized by i18next from a single key', async () => {
    const i18n = i18next.createInstance();
    await i18n.init({
      lng: 'fr',
      resources: { fr: { conversation: frTranslations } },
      defaultNS: 'conversation',
      interpolation: { prefix: '[[', suffix: ']]' },
    });

    expect(i18n.t('message.drag', { count: 1 })).toBe('1 message');
    expect(i18n.t('message.drag', { count: 3 })).toBe('3 messages');
  });
});
