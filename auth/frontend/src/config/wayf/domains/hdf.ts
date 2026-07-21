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
          acs: '/auth/login',
        },
        {
          i18n: 'wayf.relative.special',
          color: 'relative',
          acs: '/auth/login',
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
          acs: 'https://eduline.ac-lille.fr/mdp/redirectionhub/redirect.jsp?applicationname=ode_ent',
        },
        {
          i18n: 'wayf.teacher.amiens',
          color: 'teacher',
          acs: 'https://portail.ac-amiens.fr/mdp/redirectionhub/redirect.jsp?applicationname=ode_ent',
        },
        {
          i18n: 'wayf.teacher.agri',
          color: 'teacher',
          acs: '/auth/login',
        },
        {
          i18n: 'wayf.teacher.special',
          color: 'teacher',
          acs: '/auth/login',
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
          acs: 'https://eduline.ac-lille.fr/mdp/redirectionhub/redirect.jsp?applicationname=ode_ent',
        },
        {
          i18n: 'wayf.perseducnat.amiens',
          color: 'perseducnat',
          acs: 'https://portail.ac-amiens.fr/mdp/redirectionhub/redirect.jsp?applicationname=ode_ent',
        },
        {
          i18n: 'wayf.perseducnat.agri',
          color: 'perseducnat',
          acs: '/auth/login',
        },
        {
          i18n: 'wayf.perseducnat.collectivite',
          color: 'perseducnat',
          acs: '/auth/login',
        },
      ],
    },
    {
      i18n: 'wayf.other',
      color: 'other',
      icon: 'other',
      acs: '/auth/login',
    },
  ],
  partners: [
    {
      logo: '/img/partners/logo-hdf.png',
      url: 'https://www.hautsdefrance.fr/',
    },
    {
      logo: '/img/partners/logo-aisne.png',
      url: 'https://www.aisne.com/',
    },
    {
      logo: '/img/partners/logo-nord.jpg',
      url: 'https://nordcolleges.enthdf.fr/',
    },
    {
      logo: '/img/partners/logo-oise.jpg',
      url: 'https://www.oise.fr/',
    },
    {
      logo: '/img/partners/logo-pasdecalais.jpg',
      url: 'https://www.pasdecalais.fr/colleges',
    },
    {
      logo: '/img/partners/logo-somme.png',
      url: 'https://www.somme.fr/',
    },
    {
      logo: '/img/partners/logo-adica.png',
      url: 'https://www.adica.fr/assistance-conseil-informatique',
    },
    {
      logo: '/img/partners/logo-oisehd.jpg',
      url: 'https://oise-thd.fr/',
    },
    {
      logo: '/img/partners/logo-fibrenum.png',
      url: 'https://www.lafibrenumerique5962.fr/',
    },
    {
      logo: '/img/partners/logo-somme-numerique.png',
      url: 'https://www.sommenumerique.fr/',
    },
    {
      logo: '/img/partners/logo-argicole.png',
      url: 'https://draaf.hauts-de-france.agriculture.gouv.fr/',
    },
    {
      logo: '/img/partners/logo_RA_HAUTS-DE-FRANCE.png',
      url: 'https://www1.ac-lille.fr/la-region-academique-hauts-de-france-121434',
    },
    { logo: '/img/partners/logo-ue.jpg' },
  ],
};
