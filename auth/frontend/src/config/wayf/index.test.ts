import { describe, expect, it } from 'vitest';
import type { WayfProvider } from '~/models/wayf';
import { DEFAULT_WAYF_CONFIG, wayfConfig } from '.';

/**
 * A provider is valid when it has an i18n key and is either a leaf (with an
 * `acs` target) or a parent (with non-empty `children`, themselves valid).
 */
function expectValidProvider(provider: WayfProvider) {
  expect(provider.i18n).toBeTruthy();
  if ('children' in provider && provider.children) {
    expect(provider.children.length).toBeGreaterThan(0);
    provider.children.forEach(expectValidProvider);
  } else {
    expect(provider.acs).toBeTruthy();
  }
}

describe('wayfConfig', () => {
  it('has a wayf-v2 key', () => {
    expect(wayfConfig['wayf-v2']).toBeDefined();
  });

  it('returns a domain config when hostname matches', () => {
    const domainConf = wayfConfig['wayf-v2']['localhost'];
    expect(domainConf).toBeDefined();
    expect(domainConf.providers.length).toBeGreaterThan(0);
  });

  it('returns undefined for an unknown hostname', () => {
    expect(wayfConfig['wayf-v2']['unknown.example.com']).toBeUndefined();
  });
});

describe('DEFAULT_WAYF_CONFIG', () => {
  it('has 5 providers', () => {
    expect(DEFAULT_WAYF_CONFIG.providers).toHaveLength(5);
  });

  it('every provider has an i18n key and either an acs or children', () => {
    DEFAULT_WAYF_CONFIG.providers.forEach(expectValidProvider);
  });

  it('includes teacher, student, relative, perseducnat, other', () => {
    const keys = DEFAULT_WAYF_CONFIG.providers.map((p) => p.i18n);
    expect(keys).toContain('wayf.teacher');
    expect(keys).toContain('wayf.student');
    expect(keys).toContain('wayf.relative');
    expect(keys).toContain('wayf.perseducnat');
    expect(keys).toContain('wayf.other');
  });
});
