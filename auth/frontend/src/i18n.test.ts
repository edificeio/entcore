import { afterEach, describe, expect, it, vi } from 'vitest';
import { detectBrowserLanguage } from './i18n';

function mockNavigatorLanguage(language: string | undefined) {
  vi.stubGlobal('navigator', { language, languages: language ? [language] : [] });
}

describe('detectBrowserLanguage', () => {
  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it('returns the base code of a supported browser language', () => {
    mockNavigatorLanguage('en-US');
    expect(detectBrowserLanguage()).toBe('en');
  });

  it('is case-insensitive', () => {
    mockNavigatorLanguage('ES-es');
    expect(detectBrowserLanguage()).toBe('es');
  });

  it('falls back to French for an unsupported language', () => {
    mockNavigatorLanguage('nl-NL');
    expect(detectBrowserLanguage()).toBe('fr');
  });

  it('falls back to French when no browser language is available', () => {
    mockNavigatorLanguage(undefined);
    expect(detectBrowserLanguage()).toBe('fr');
  });
});
