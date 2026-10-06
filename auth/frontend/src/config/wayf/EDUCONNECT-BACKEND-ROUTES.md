# État des lieux EduConnect — routes backend `/auth/saml/authn/*`

Date de l'audit : 2026-10-06 (mise à jour 2026-10-06 : ajout MonCollège, MonÉcole, Porto Vecchio)
Destinataire : David (SRE)
Périmètre : les 25 domaines WAYF v2 traités par la moulinette [[wayf-domain-config]] (`auth/frontend/src/config/wayf/domains/*.ts`).

Deux façons d'accéder à EduConnect sont utilisées dans les confs :
1. **URL EduConnect directe** (`https://educonnect.education.gouv.fr/idp/profile/SAML2/Unsolicited/SSO?providerId=...`) — le profil (élève ou parent) n'est pas présélectionné, l'utilisateur doit le choisir sur EduConnect.
2. **Route backend locale** `/auth/saml/authn/student` ou `/auth/saml/authn/relative` — redirige vers EduConnect avec le bon profil déjà présélectionné, mais nécessite que la route soit activée côté back pour le domaine concerné.

## Résumé

- **13 entrées** utilisent la route backend `/auth/saml/authn/student` ou `/auth/saml/authn/relative`, **toutes confirmées fonctionnelles**.
- **1 domaine** (**Porto Vecchio**) utilise un pattern similaire mais pour un **autre fournisseur d'identité** (Lià, pas EduConnect) via la route backend `/auth/openid/login` — voir section dédiée.
- **19 entrées** (réparties sur 13 domaines) utilisent encore l'URL EduConnect directe (sans présélection de profil).
- **5 domaines** (Guyane pour les branches école/agricole, La Réunion, Vienne-Condrieu, Reims pour l'élève, E-Primo pour l'élève) n'ont aucune entrée EduConnect pour élève et/ou parent — connexion locale uniquement, par choix produit confirmé pour Reims et E-Primo.

> **Mise à jour 2026-10-06** : le cas E-Primo (élève) initialement signalé comme une route backend cassée (403) a été reclassé — retour produit confirmé que `/auth/login` est le comportement voulu pour ce profil sur ce domaine, pas une activation backend en attente. Plus aucune action urgente sur ce rapport.

## Tableau 1 — Routes backend `/auth/saml/authn/*` (présélection de profil)

| Domaine | Profil | URL | Statut |
| --- | --- | --- | --- |
| AuCollège84 (Vaucluse) | Élève | `/auth/saml/authn/student` | ✅ Confirmée (trouvée en live sur l'ancienne WAYF) |
| AuCollège84 (Vaucluse) | Parent | `/auth/saml/authn/relative` | ✅ Confirmée (trouvée en live) |
| Guadeloupe (Karukera) | Élève | `/auth/saml/authn/student` | ✅ Confirmée (trouvée en live) |
| Guadeloupe (Karukera) | Parent | `/auth/saml/authn/relative` | ✅ Confirmée (trouvée en live) |
| HDF | Élève (collège/lycée) | `/auth/saml/authn/student` | ✅ Confirmée (trouvée en live) |
| HDF | Parent (école/collège/lycée) | `/auth/saml/authn/relative` | ✅ Confirmée (trouvée en live) |
| NATI | Élève | `/auth/saml/authn/student` | ✅ Confirmée (trouvée en live) |
| NATI | Parent | `/auth/saml/authn/relative` | ✅ Confirmée (trouvée en live) |
| PrimOT | Parent | `/auth/saml/authn/relative` | ✅ Confirmée (trouvée en live) |
| E-Primo | Parent | `/auth/saml/authn/relative` | ✅ Confirmée (testée le 2026-10-06) |
| MonCollège (Essonne) | Élève | `/auth/saml/authn/student` | ✅ Confirmée (trouvée en live) |
| MonCollège (Essonne) | Parent | `/auth/saml/authn/relative` | ✅ Confirmée (trouvée en live) |
| MonÉcole (Essonne) | Parent | `/auth/saml/authn/relative` | ✅ Confirmée (trouvée en live) |

## Porto Vecchio — autre fournisseur d'identité (Lià, pas EduConnect)

Porto Vecchio utilise le même *pattern architectural* (route backend pivot qui redirige vers un fournisseur d'identité externe avec un profil présélectionné), mais pour un fournisseur **différent d'EduConnect** : **Lià** (`connect.monespacedemarches.fr`, protocole OpenID Connect), réservé au profil **Parent** uniquement.

| Domaine | Profil | URL | Statut |
| --- | --- | --- | --- |
| Porto Vecchio | Parent | `/auth/openid/login` | ✅ Confirmée (trouvée en live, re-testée le 2026-10-06 — redirige vers `connect.monespacedemarches.fr` avec un nouveau `state`/`nonce` à chaque appel) |

Pas d'action backend nécessaire : la route est déjà active et fonctionnelle. À noter pour David car c'est un fournisseur OIDC tiers (pas le SSO national EduConnect), au cas où ça impacte une éventuelle supervision/monitoring des routes `/auth/*`.

## Tableau 2 — URL EduConnect directe (pas de présélection de profil)

| Domaine | Profil(s) concerné(s) | Repère (providerId / métadonnées) |
| --- | --- | --- |
| EduProvence (CD13) | Élève + Parent | `prod-cd13-edu` |
| ENT04 | Élève + Parent | `prod-ent04-edu` |
| Hautes-Alpes (ENT05) | Élève + Parent | `prod-ent05-edu` |
| Guyane (Wilapa) | Élève + Parent (branche "Collège ou Lycée" uniquement) | métadonnées `wilapa-guyane.com` |
| LEIA | Élève + Parent | `prod-leia-edu` |
| Colibri (Martinique) | Élève (branche "Du CM2 au lycée") + Parent | métadonnées `colibri.ac-martinique.fr` |
| Mayotte | Élève + Parent (branche EduConnect, en plus du "Compte local ENT") | `prod-edifice-mayotte-edu` |
| Ariane57 (Moselle) | Parent uniquement (pas d'élève EduConnect) | `prod-ent57-edu` |
| Éduc Normandie | Élève (×2 : école/compte local + collège-lycée) + Parent (×2, même structure) | métadonnées `ent.l-educdenormandie.fr` |
| ENT Écoles Reims | Parent uniquement (élève → compte local ENT, voir ci-dessous) | `prod-reims-1d-edu` |
| SEM | Parent uniquement (branche "École publique") | `prod-sem-edu` |
| Toulon | Parent uniquement | `prod-toulon-edu` |
| VAR | Élève + Parent | `prod-cd83-edu` |
| PandaVosges | Parent uniquement | `prod-vosges-edu` |

## Domaines sans EduConnect du tout

| Domaine | Détail |
| --- | --- |
| La Réunion | Élève et Parent → `/auth/login` (pas d'EduConnect) |
| Vienne-Condrieu (VCA) | Élève et Parent → `/auth/login` |
| Guyane | Branches "École" et "Établissement agricole" → `/auth/login` (seule la branche "Collège ou Lycée" passe par EduConnect) |
| ENT Écoles Reims | Élève → `/auth/login` (compte local ENT) — décision Pascal du 2026-10-06, l'ancienne WAYF faisait déjà ça nativement |
| E-Primo | Élève → `/auth/login` — route backend `/auth/saml/authn/student` testée en 403, puis **confirmé par retour produit (2026-10-06)** que c'est le comportement voulu, pas une activation backend en attente |

## Actions à mener (synthèse)

Aucune action urgente. **Optionnel / roadmap** : pour les 13 domaines du Tableau 2, activer `/auth/saml/authn/student` et/ou `/auth/saml/authn/relative` côté back si on veut généraliser la présélection de profil — rien n'est cassé aujourd'hui, c'est juste l'ancienne méthode (URL EduConnect directe, sans présélection).
