import { describe, expect, it } from 'vitest';
import { DEFAULT_WAYF_CONFIG, wayfConfig } from './wayf.config';

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

  it('every provider has an i18n key and an acs', () => {
    DEFAULT_WAYF_CONFIG.providers.forEach((p) => {
      expect(p.i18n).toBeTruthy();
      expect(p.acs).toBeTruthy();
    });
  });

  it('includes teacher, student, parent, personnel, guest', () => {
    const keys = DEFAULT_WAYF_CONFIG.providers.map((p) => p.i18n);
    expect(keys).toContain('wayf.teacher');
    expect(keys).toContain('wayf.student');
    expect(keys).toContain('wayf.parent');
    expect(keys).toContain('wayf.personnel');
    expect(keys).toContain('wayf.guest');
  });
});
