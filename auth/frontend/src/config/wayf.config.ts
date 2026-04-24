import type { WayfConfig, WayfDomainConfig } from '~/models/wayf';

export const DEFAULT_WAYF_CONFIG: WayfDomainConfig = {
  providers: [
    {
      i18n: 'wayf.teacher',
      icon: 'teacher',
      acs: '/auth/saml/default-teacher',
    },
    {
      i18n: 'wayf.student',
      icon: 'student',
      acs: '/auth/saml/default-student',
    },
    { i18n: 'wayf.parent', icon: 'relative', acs: '/auth/saml/default-parent' },
    {
      i18n: 'wayf.personnel',
      icon: 'perseducnat',
      acs: '/auth/saml/default-personnel',
    },
    { i18n: 'wayf.guest', icon: 'other', acs: '/auth/saml/default-guest' },
  ],
};

const hdfProviders: WayfDomainConfig = {
  providers: [
    {
      i18n: 'wayf.student',
      icon: 'student',
      titleI18n: 'wayf.select.level',
      children: [
        { i18n: 'wayf.student.ecole', acs: '/auth/saml/student-ecole' },
        {
          i18n: 'wayf.student.college-lycee',
          acs: '/auth/saml/student-college-lycee',
        },
        { i18n: 'wayf.student.agri', acs: '/auth/saml/student-agri' },
        { i18n: 'wayf.student.special', acs: '/auth/saml/student-special' },
      ],
    },
    {
      i18n: 'wayf.relative',
      icon: 'relative',
      titleI18n: 'wayf.select.level',
      children: [
        {
          i18n: 'wayf.relative.ecole-college-lycee',
          acs: '/auth/saml/relative-ecole-college-lycee',
        },
        { i18n: 'wayf.relative.agri', acs: '/auth/saml/relative-agri' },
        { i18n: 'wayf.relative.special', acs: '/auth/saml/relative-special' },
      ],
    },
    {
      i18n: 'wayf.teacher',
      icon: 'teacher',
      titleI18n: 'wayf.select.academy',
      children: [
        { i18n: 'wayf.teacher.lille', acs: '/auth/saml/teacher-lille' },
        { i18n: 'wayf.teacher.amiens', acs: '/auth/saml/teacher-amiens' },
        { i18n: 'wayf.teacher.agri', acs: '/auth/saml/teacher-agri' },
        { i18n: 'wayf.teacher.special', acs: '/auth/saml/teacher-special' },
      ],
    },
    {
      i18n: 'wayf.perseducnat',
      icon: 'perseducnat',
      titleI18n: 'wayf.choice',
      children: [
        { i18n: 'wayf.perseducnat.lille', acs: '/auth/saml/perseducnat-lille' },
        {
          i18n: 'wayf.perseducnat.amiens',
          acs: '/auth/saml/perseducnat-amiens',
        },
        { i18n: 'wayf.perseducnat.agri', acs: '/auth/saml/perseducnat-agri' },
        {
          i18n: 'wayf.perseducnat.collectivite',
          acs: '/auth/saml/perseducnat-collectivite',
        },
      ],
    },
    { i18n: 'wayf.other', icon: 'other', acs: '/auth/saml/other' },
  ],
};

export const wayfConfig: WayfConfig = {
  'wayf-v2': {
    'connexion.enthdf.fr': hdfProviders,
    'localhost': hdfProviders,
  },
};
