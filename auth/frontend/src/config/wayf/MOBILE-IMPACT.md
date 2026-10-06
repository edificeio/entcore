# Impact mobile des migrations WAYF personnalisée (ENABLING-1215)

Date de l'audit : 2026-10-06 (mise à jour 2026-10-06 : ajout MonCollège, MonÉcole, Porto Vecchio)
Repo mobile analysé : `edifice-mobile-config` (`common/platforms/*.ts` + `flash/src/share/utils/EdificePlatforms.ts`)
Méthode : `grep -rn "wayf:"` sur tout le repo, puis comparaison de chaque URL avec la config WAYF v2 réellement écrite côté `entcore` pour les 25 domaines traités par la moulinette [[wayf-domain-config]].

## Résumé

- **22 domaines sur 25** : l'URL `wayf` déjà présente côté mobile correspond **exactement** à l'URL utilisée dans notre config WAYF v2 (`<domaine>/auth/saml/wayf`). **Aucun changement requis.**
- **2 domaines** utilisent aujourd'hui, côté mobile, un **portail de connexion personnalisé séparé** (pas la route standard `/auth/saml/wayf`) : **Éduc Normandie** et **Ariane57**. Changement d'URL à faire + redirection 301 à prévoir par le SRE.
- **1 domaine** (**Porto Vecchio**) a une URL `wayf` **pas encore activée côté mobile** (commentée ou absente) — la conf WAYF v2 vient seulement d'être écrite, pas une divergence, juste une activation à faire.

> Hypothèse de travail (à confirmer avec l'équipe mobile) : "les migrations qui impactent le mobile" désignent précisément ces domaines où l'ancien système était un portail maison, remplacé par la route Edifice standard — pas les domaines qui utilisaient déjà `/auth/saml/wayf` et n'ont fait que changer de rendu (AngularJS → React) à la même URL.

## Les 2 divergences réelles

### Éduc Normandie

- **URL mobile actuelle** : `https://connexion.l-educdenormandie.fr` (portail JS custom, domaine entièrement différent du domaine principal)
- **Notre route WAYF v2** : `https://ent.l-educdenormandie.fr/auth/saml/wayf`
- **Fichiers mobile à modifier** :
  - `common/platforms/normandie.ts` (clé `prod.wayf`)
  - `flash/src/share/utils/EdificePlatforms.ts` (clé `prod-normandie.wayf`)
- **Action SRE** : prévoir une redirection 301 de `connexion.l-educdenormandie.fr` vers la nouvelle URL (ou confirmer le sort de l'ancien portail).

### Ariane57 (Moselle)

- **URL mobile actuelle** : `https://ariane57.moselle-education.fr/connexion` — un portail custom à 3 profils (vérifié en live le 2026-10-06) : "Parents et Élèves" / "Enseignants" / "Invités et Personnels". Structure plus simple que les 5 catégories de notre conf Confluence.
- **Notre route WAYF v2** : `https://ariane57.moselle-education.fr/auth/saml/wayf`
- **⚠️ Prérequis bloquant** : cette route est aujourd'hui **vide** (aucun provider SAML configuré côté académie) — voir mémoire `project-wayf-ariane57-blocked`. Le changement mobile ne doit pas être fait avant l'activation backend complète (l'URL ARENA, elle, a déjà été corrigée le 2026-10-06).
- **Fichiers mobile à modifier** :
  - `common/platforms/saas.ts` (clé `prod-ariane.wayf`)
  - `flash/src/share/utils/EdificePlatforms.ts` (clé `prod-ariane.wayf`)
- **Action SRE** : prévoir une redirection 301 de `ariane57.moselle-education.fr/connexion` vers la nouvelle URL, une fois le backend prêt.

## Porto Vecchio — activation à faire (pas une divergence)

- **`common/platforms/saas.ts`** (`prod-porto-vecchio`) : la clé `wayf` existe déjà mais est **commentée** :
  `//wayf: "https://portivechju.edifice.io/auth/saml/wayf?#",` — l'URL est correcte (identique à notre conf WAYF v2), il suffit de **décommenter**.
- **`flash/src/share/utils/EdificePlatforms.ts`** (`prod-porto-vecchio`) : la clé `wayf` est **totalement absente** — à **ajouter** : `wayf: 'https://portivechju.edifice.io/auth/saml/wayf',`.
- Pas de redirection 301 nécessaire (aucune ancienne URL à rediriger, c'est une première activation).

## Détail des 25 domaines

| # | Domaine | Fichier(s) config mobile | URL `wayf` mobile actuelle | Correspond à notre conf ? | Action |
| - | - | - | - | - | - |
| 1 | Guadeloupe (Karukera) | `saas.ts` | `karukera.ac-guadeloupe.fr/auth/saml/wayf` | ✅ | Aucune |
| 2 | ENT04 | `saas.ts` | `ent04.fr/auth/saml/wayf` | ✅ | Aucune |
| 3 | PrimOT | `mutu.ts` | `www.primot.fr/auth/saml/wayf` | ✅ | Aucune |
| 4 | SEM (E-cléo) | `saas.ts` | `sem.edifice.io/auth/saml/wayf` | ✅ | Aucune |
| 5 | LEIA | `mutu.ts` | `ent.leia.corsica/auth/saml/wayf` | ✅ | Aucune |
| 6 | **Éduc Normandie** | `normandie.ts` + `flash` | `connexion.l-educdenormandie.fr` | ❌ domaine différent | **À changer + 301** |
| 7 | VCA | `saas.ts` | `ent.vienne-condrieu-agglomeration.fr/auth/saml/wayf` | ✅ | Aucune |
| 8 | Wilapa (Guyane) | `saas.ts` (×2 entrées) | `wilapa-guyane.com/auth/saml/wayf` | ✅ | Aucune |
| 9 | HDF | `hdf.ts` | `enthdf.fr/auth/saml/wayf` | ✅ | Aucune |
| 10 | NATI | `saas.ts` (×2, natitahi/natirua) | `nati.pf/auth/saml/wayf` | ✅ | Aucune |
| 11 | Hautes-Alpes (ENT05) | `saas.ts` | `ent.colleges05.fr/auth/saml/wayf` | ✅ | Aucune |
| 12 | VAR | `saas.ts` | `moncollege-ent.var.fr/auth/saml/wayf` | ✅ | Aucune |
| 13 | PandaVosges | `saas.ts` | `panda.vosges.fr/auth/saml/wayf` | ✅ | Aucune |
| 14 | EduProvence (CD13) | `saas.ts` | `www.eduprovence.fr/auth/saml/wayf` | ✅ | Aucune |
| 15 | **Ariane57 (Moselle)** | `saas.ts` + `flash` | `ariane57.moselle-education.fr/connexion` | ❌ portail séparé | **À changer + 301** (attend activation backend) |
| 16 | AuCollège84 (Vaucluse) | `saas.ts` | `www.aucollege84.vaucluse.fr/auth/saml/wayf` | ✅ | Aucune |
| 17 | Colibri (Martinique) | `colibri.ts` (×4 entrées) | `colibri.ac-martinique.fr/auth/saml/wayf` | ✅ | Aucune |
| 18 | ENT Écoles Reims | `saas.ts` | `ent-ecoles.ac-reims.fr/auth/saml/wayf` | ✅ | Aucune |
| 19 | E-Primo | `saas.ts` | `ent.e-primo.fr/auth/saml/wayf` | ✅ | Aucune |
| 20 | La Réunion | `reunion.ts` | `ent1d.ac-reunion.fr/auth/saml/wayf` | ✅ | Aucune |
| 21 | Mayotte | `saas.ts` | `mayotte.edifice.io/auth/saml/wayf` | ✅ | Aucune |
| 22 | Toulon | `saas.ts` | `ent.toulon.fr/auth/saml/wayf` | ✅ | Aucune |
| 23 | MonCollège (Essonne) | `moncollege.ts` | `www.moncollege-ent.essonne.fr/auth/saml/wayf` | ✅ | Aucune |
| 24 | MonÉcole (Essonne) | `moncollege.ts` (clé `prod-monecole`) | `monecole-ent.essonne.fr/auth/saml/wayf` | ✅ | Aucune |
| 25 | **Porto Vecchio** | `saas.ts` + `flash` | commentée / absente | ⚠️ pas activée | **Décommenter / ajouter** (voir section dédiée ci-dessus) |

## Prochaines étapes (selon la note de Pascal du 2026-10-06)

1. ~~Lire `common/platforms/*.ts` pour repérer les configs à `wayf:`~~ — fait, voir ci-dessus.
2. ~~Compulser les configs qui demandent un changement~~ — fait : Normandie, Ariane57, et Porto Vecchio (activation).
3. Prévenir Guillaume des changements (Normandie + Ariane57 + Porto Vecchio, avec le détail des fichiers et clés à modifier ci-dessus).
4. Prévoir avec le SRE les redirections 301 :
   - `connexion.l-educdenormandie.fr` → `ent.l-educdenormandie.fr/auth/saml/wayf`
   - `ariane57.moselle-education.fr/connexion` → `ariane57.moselle-education.fr/auth/saml/wayf` (une fois le backend SAML activé pour ce domaine)
   - Porto Vecchio : pas de redirection nécessaire, juste une activation de clé déjà présente/à ajouter.
