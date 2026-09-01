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
    { logo: '/img/partners/77 college connecte@4x.png' },
    { logo: '/img/partners/Alpes de haute provence NEO@2x.png' },
    { logo: '/img/partners/Bouche rhone neo@2x.png' },
    { logo: '/img/partners/ENT HDF one neo.png' },
    { logo: '/img/partners/Hautes-alpesNEO@2x.png' },
    { logo: '/img/partners/Logo Colibri (1).png' },
    { logo: '/img/partners/LogoADM_NEO_couleur@4x.png' },
    { logo: '/img/partners/Noir - logo-college-var.png' },
    { logo: '/img/partners/Odyssey-byNEO@2x.png' },
    { logo: '/img/partners/PCN Edifice Academie sans fond.png' },
    { logo: '/img/partners/Plan de travail 1@1.5x.png' },
    { logo: '/img/partners/charente by neo.png' },
    { logo: '/img/partners/e-primo logo.png' },
    { logo: '/img/partners/educnormandie x6.png' },
    { logo: '/img/partners/espace collegien de provence by neo@2x.png' },
    { logo: '/img/partners/image (1).png' }
  ],
};
