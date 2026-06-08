import type { WayfDomainConfig } from '~/models/wayf';

/**
 * Fallback configuration used when the resolved domain has no dedicated
 * config (see `~/config/wayf`). Keys must match real entries in the `auth`
 * i18n file (`auth/src/main/resources/i18n/*.json`).
 */
export const DEFAULT_WAYF_CONFIG: WayfDomainConfig = {
  providers: [
    {
      i18n: 'wayf.teacher',
      color: 'teacher',
      icon: 'teacher',
      acs: '/auth/login',
    },
    {
      // Level 2 example: student → ecole / college-lycee
      i18n: 'wayf.student',
      color: 'student',
      icon: 'student',
      children: [
        {
          i18n: 'wayf.student.ecole',
          color: 'student',
          acs: '/auth/login',
        },
        {
          // Level 3 example: college-lycee → public / prive
          i18n: 'wayf.student.college-lycee',
          color: 'student',
          children: [
            {
              i18n: 'wayf.student.college-lycee.public',
              color: 'student',
              acs: '/auth/login',
            },
            {
              i18n: 'wayf.student.college-lycee.prive',
              color: 'student',
              acs: '/auth/login',
            },
          ],
        },
      ],
    },
    {
      i18n: 'wayf.relative',
      color: 'relative',
      icon: 'relative',
      acs: '/auth/login',
    },
    {
      i18n: 'wayf.perseducnat',
      color: 'perseducnat',
      icon: 'perseducnat',
      acs: '/auth/login',
    },
    {
      i18n: 'wayf.other',
      color: 'other',
      icon: 'other',
      acs: '/auth/login',
    },
  ],
};
