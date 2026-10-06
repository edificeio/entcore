import type { WayfDomainConfig } from '~/models/wayf';

// NOTE: URLs extraites de /auth/login (pas de /auth/saml/wayf fonctionnel pour
// ce domaine — cf. ENABLING-1218). ARENA_ACS vérifiée le 2026-10-06 (corrigée
// depuis la version initiale du 2026-09-30, qui était un simple lien de
// portail non fonctionnel) : redirige bien vers une vraie page de connexion
// ARENA.
const EDUCONNECT_ACS =
  'https://educonnect.education.gouv.fr/idp/profile/SAML2/Unsolicited/SSO?providerId=urn%3Afi%3Aent%3Aprod-ent57-edu%3A1.0';
const ARENA_ACS =
  'https://portail.ac-nancy-metz.fr/sso/SSO?SPEntityID=urn:fi:ent:prod-ent57-aaa:1.0&TARGET=https://ariane57.moselle-education.fr';

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
