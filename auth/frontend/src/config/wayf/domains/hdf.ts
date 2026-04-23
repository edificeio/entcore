import type { WayfDomainConfig } from '~/models/wayf';

export const hdfConfig: WayfDomainConfig = {
  providers: [
    {
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
          i18n: 'wayf.student.college-lycee',
          color: 'student',
          acs: '/auth/saml/authn/student',
        },
        {
          i18n: 'wayf.student.agri',
          color: 'student',
          acs: '/auth/login',
        },
        {
          i18n: 'wayf.student.special',
          color: 'student',
          acs: '/auth/login',
        },
      ],
    },
    {
      i18n: 'wayf.relative',
      color: 'relative',
      icon: 'relative',
      children: [
        {
          i18n: 'wayf.relative.ecole-college-lycee',
          color: 'relative',
          acs: '/auth/saml/authn/relative',
        },
        {
          i18n: 'wayf.relative.agri',
          color: 'relative',
          acs: '/auth/saml/relative-agri',
        },
        {
          i18n: 'wayf.relative.special',
          color: 'relative',
          acs: '/auth/saml/relative-special',
        },
      ],
    },
    {
      i18n: 'wayf.teacher',
      color: 'teacher',
      icon: 'teacher',
      children: [
        {
          i18n: 'wayf.teacher.lille',
          color: 'teacher',
          acs: '/auth/saml/teacher-lille',
        },
        {
          i18n: 'wayf.teacher.amiens',
          color: 'teacher',
          acs: '/auth/saml/teacher-amiens',
        },
        {
          i18n: 'wayf.teacher.agri',
          color: 'teacher',
          acs: '/auth/saml/teacher-agri',
        },
        {
          i18n: 'wayf.teacher.special',
          color: 'teacher',
          acs: '/auth/saml/teacher-special',
        },
      ],
    },
    {
      i18n: 'wayf.perseducnat',
      color: 'perseducnat',
      icon: 'perseducnat',
      children: [
        {
          i18n: 'wayf.perseducnat.lille',
          color: 'perseducnat',
          acs: '/auth/saml/perseducnat-lille',
        },
        {
          i18n: 'wayf.perseducnat.amiens',
          color: 'perseducnat',
          acs: '/auth/saml/perseducnat-amiens',
        },
        {
          i18n: 'wayf.perseducnat.agri',
          color: 'perseducnat',
          acs: '/auth/saml/perseducnat-agri',
        },
        {
          i18n: 'wayf.perseducnat.collectivite',
          color: 'perseducnat',
          acs: '/auth/saml/perseducnat-collectivite',
        },
      ],
    },
    {
      i18n: 'wayf.other',
      color: 'other',
      icon: 'other',
      acs: '/auth/saml/other',
    },
  ],
  partners: [
    {
      logo: '/assets/themes/cg77/img/partners/logo-hdf.png',
      url: 'https://www.hautsdefrance.fr/',
    },
    {
      logo: '/assets/themes/cg77/img/partners/logo-aisne.png',
      url: 'https://www.aisne.com/',
    },
    {
      logo: '/assets/themes/cg77/img/partners/logo-nord.jpg',
      url: 'https://nordcolleges.enthdf.fr/',
    },
    {
      logo: '/assets/themes/cg77/img/partners/logo-oise.jpg',
      url: 'https://www.oise.fr/',
    },
    {
      logo: '/assets/themes/cg77/img/partners/logo-pasdecalais.jpg',
      url: 'https://www.pasdecalais.fr/colleges',
    },
    {
      logo: '/assets/themes/cg77/img/partners/logo-somme.png',
      url: 'https://www.somme.fr/',
    },
    {
      logo: '/assets/themes/cg77/img/partners/logo-adica.png',
      url: 'https://www.adica.fr/assistance-conseil-informatique',
    },
    {
      logo: '/assets/themes/cg77/img/partners/logo-oisehd.jpg',
      url: 'https://oise-thd.fr/',
    },
    {
      logo: '/assets/themes/cg77/img/partners/logo-fibrenum.png',
      url: 'https://www.lafibrenumerique5962.fr/',
    },
    {
      logo: '/assets/themes/cg77/img/partners/logo-somme-numerique.png',
      url: 'https://www.sommenumerique.fr/',
    },
    {
      logo: '/assets/themes/cg77/img/partners/logo-argicole.png',
      url: 'https://draaf.hauts-de-france.agriculture.gouv.fr/',
    },
    {
      logo: '/assets/themes/cg77/img/partners/logo_RA_HAUTS-DE-FRANCE.png',
      url: 'https://www1.ac-lille.fr/la-region-academique-hauts-de-france-121434',
    },
    { logo: '/assets/themes/cg77/img/partners/logo-ue.jpg' },
  ],
};
