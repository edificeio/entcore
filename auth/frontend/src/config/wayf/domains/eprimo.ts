import type { WayfDomainConfig } from '~/models/wayf';

// NOTE: Confluence demandait <student> → EduConnect élève, mais
// /auth/saml/authn/student répond 403 sur ce domaine (pas de fédération
// EduConnect élève côté back). Confirmé par retour produit (2026-10-06) :
// /auth/login est bien le comportement voulu pour <student> ici, pas une
// activation backend en attente — même décision que sur Reims.
const ARENA_ACS =
  'https://aaa-idp.ac-nantes.fr/sso/SSO?SPEntityID=urn:fi:ent:prod-eprimo:1.0&TARGET=https://ent.e-primo.fr';

export const eprimoConfig: WayfDomainConfig = {
  edificeLogoClickable: false,
  providers: [
    {
      i18n: 'wayf.student',
      color: 'student',
      icon: 'student',
      acs: '/auth/login',
    },
    {
      i18n: 'wayf.teacher',
      color: 'teacher',
      icon: 'teacher',
      acs: ARENA_ACS,
    },
    {
      i18n: 'wayf.relative',
      color: 'relative',
      icon: 'relative',
      acs: '/auth/saml/authn/relative',
    },
    {
      i18n: 'wayf.other',
      color: 'other',
      icon: 'other',
      acs: '/auth/login',
    },
    {
      i18n: 'wayf.perseducnat',
      color: 'perseducnat',
      icon: 'perseducnat',
      acs: '/auth/login',
    },
  ],
};
