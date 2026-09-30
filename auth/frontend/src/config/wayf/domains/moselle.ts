import type { WayfDomainConfig } from '~/models/wayf';

// NOTE: URLs ARENA/EduConnect extraites de /auth/login (pas de /auth/saml/wayf
// fonctionnel pour ce domaine, fédération SAML pas encore activée côté
// académie — cf. ENABLING-1218). Probablement provisoires, à corriger une fois
// la fédération réellement en place ; posées ainsi pour permettre un premier
// test en recette.
const EDUCONNECT_ACS =
  'https://educonnect.education.gouv.fr/idp/profile/SAML2/Unsolicited/SSO?providerId=urn%3Afi%3Aent%3Aprod-ent57-edu%3A1.0';
const ARENA_ACS = 'https://login.ac-nancy-metz.fr/';

export const moselleConfig: WayfDomainConfig = {
  providers: [
    {
      i18n: 'wayf.student',
      color: 'student',
      icon: 'student',
      acs: '/auth/login',
    },
    {
      i18n: 'wayf.relative',
      color: 'relative',
      icon: 'relative',
      children: [
        {
          i18n: 'wayf.relative.local',
          color: 'relative',
          acs: '/auth/login',
        },
        {
          i18n: 'wayf.relative.educonnect',
          color: 'relative',
          acs: EDUCONNECT_ACS,
        },
      ],
    },
    {
      i18n: 'wayf.teacher',
      color: 'teacher',
      icon: 'teacher',
      children: [
        {
          i18n: 'wayf.teacher.local',
          color: 'teacher',
          acs: '/auth/login',
        },
        {
          i18n: 'wayf.teacher.arena',
          color: 'teacher',
          acs: ARENA_ACS,
        },
      ],
    },
    {
      i18n: 'wayf.perseducnat',
      color: 'perseducnat',
      icon: 'perseducnat',
      children: [
        {
          i18n: 'wayf.perseducnat.academy',
          color: 'perseducnat',
          acs: ARENA_ACS,
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
    { logo: '/img/partners/logo-moselle.png', url: 'https://www.moselle.fr/' },
    { logo: '/img/partners/logo-ac-nancy-metz.png', url: 'https://www.ac-nancy-metz.fr/' },
  ],
};
