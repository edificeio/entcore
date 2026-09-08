import type { WayfDomainConfig } from '~/models/wayf';

export const neoconnectConfig: WayfDomainConfig = {
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
        {
          i18n: 'wayf.local',
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
        {
          i18n: 'wayf.local',
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
          i18n: 'wayf.teacher.academie',
          color: 'teacher',
          acs: 'https://eduline.ac-lille.fr/mdp/redirectionhub/redirect.jsp?applicationname=ode_ent',
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
        {
          i18n: 'wayf.local',
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
          i18n: 'wayf.perseducnat.academie',
          color: 'perseducnat',
          acs: 'https://eduline.ac-lille.fr/mdp/redirectionhub/redirect.jsp?applicationname=ode_ent',
        },
        {
          i18n: 'wayf.perseducnat.collectivite',
          color: 'perseducnat',
          acs: '/auth/login',
        },
        {
          i18n: 'wayf.perseducnat.agri',
          color: 'perseducnat',
          acs: '/auth/login',
        },
        {
          i18n: 'wayf.local',
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

    { logo: '/img/partners/REGIONBFC.jpeg' },
    { logo: '/img/partners/CD_blue_2018fin.png' },
    { logo: "/img/partners/Côte-d'Or_(21)_logo_2015.svg" },
    { logo: '/img/partners/Logo-CD89-2022-couleur-PNG-.png' },
    { logo: '/img/partners/Logo_Conseil_départemental_du_Jura.jpg' },
    { logo: '/img/partners/25_departement-doubs-logo.png' },
    { logo: '/img/partners/HAUTE-SAONE.png' },
    { logo: '/img/partners/territoiredebelfort_logo_rvb.jpg' },
        { logo: '/img/partners/02_logo_REGIONS-ACA_BOURGOGNE-FRANCHE-COMTE.webp' },
    { logo: '/img/partners/logoAC_Dijon2.svg' },
    { logo: '/img/partners/Académie_de_Besançon.svg.webp' },
    { logo: '/img/partners/draaf2.png' },
  ],
};

