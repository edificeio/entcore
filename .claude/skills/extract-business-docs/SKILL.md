---
name: extract-business-docs
description: |
  Extrait les règles métier d'une application entcore (module du monorepo /Volumes/Work/entcore) et génère 5 documents de spécification structurés. Utilise cette skill CHAQUE FOIS qu'on demande d'extraire les règles métier, documenter une app, analyser la logique métier, générer des specs, ou préparer la doc d'un module entcore (ex. "extrais les règles métier de conversation", "documente timeline", "analyse auth pour la migration"). NE PAS utiliser pour écrire du code ou rédiger une user story.
allowed-tools: Read, Glob, Grep, Write, Bash(find *), Bash(ls *), Bash(mkdir *), Bash(date *)
---

# Extraction des documents métier — entcore monorepo

Lire le code d'une application du monorepo entcore pour produire une documentation métier **structurée et sourcée**. Ces specs servent de référence pour la migration React, la QA et l'onboarding.

## Étape 0 — Identifier le module cible

Détermine le nom du module (`<APP>`) depuis le contexte de la conversation.
Apps connues : `admin`, `app-registry`, `archive`, `audience`, `auth`, `cas`, `common`, `communication`, `conversation`, `directory`, `feeder`, `infra`, `portal`, `session`, `timeline`, `workspace`.

Si `<APP>` n'est pas clair, affiche la liste et demande.

Racine du module : `/Volumes/Work/entcore/<APP>/`

## Étape 1 — Détecter le pattern structurel

Inspecte la racine du module pour déterminer son layout :

```
ls /Volumes/Work/entcore/<APP>/
```

Trois cas possibles — note lequel s'applique, car les chemins diffèrent :

### Pattern A — Mono legacy (ex. `directory`, `workspace`, `admin`)
Présence de `src/` sans `frontend/` ni `backend/`.
```
<APP>/src/main/java/org/entcore/<APP>/          ← backend Vert.x/Java
<APP>/src/main/resources/public/ts/             ← frontend AngularJS (TypeScript compilé)
<APP>/src/main/resources/public/js/             ← JS legacy (si pas de ts/)
<APP>/src/main/resources/i18n/                  ← libellés (indices de features)
<APP>/src/main/resources/jsonschema/            ← schémas JSON (modèles)
<APP>/src/main/resources/view-src/              ← (si présent) pont routage AngularJS→React
```

### Pattern B — Split migré (ex. `conversation`, `portal`)
Présence de `backend/` et `frontend/`.
```
<APP>/backend/src/main/java/org/entcore/<APP>/ ← backend Vert.x/Java
<APP>/backend/src/main/resources/public/js/    ← JS legacy résiduel
<APP>/backend/src/main/resources/i18n/         ← libellés
<APP>/backend/src/main/resources/jsonschema/   ← schémas JSON
<APP>/frontend/src/features/                   ← React — features (point d'entrée principal)
<APP>/frontend/src/models/                     ← types TypeScript (modèles de données)
<APP>/frontend/src/services/api/               ← appels API REST
<APP>/frontend/src/services/queries/           ← TanStack Query (hooks de données)
<APP>/frontend/src/routes/                     ← routing React + redirections
<APP>/frontend/src/store/                      ← état global Zustand
<APP>/frontend/src/hooks/                      ← hooks custom
<APP>/frontend/src/components/                 ← composants partagés
```

### Pattern C — Transitional (ex. `auth`, `timeline`)
Présence de `src/` ET `frontend/` : le legacy coexiste avec la nouvelle app React.
Utiliser les chemins du Pattern A pour le legacy, et ceux du Pattern B pour `frontend/`.

## Étape 2 — Explorer et cartographier les features

Avant d'analyser, lister ce qui existe selon le pattern détecté.
Pour le Pattern B/C, lister `frontend/src/features/` — chaque sous-dossier est souvent une feature.
Pour le Pattern A/C legacy, lister `src/main/resources/public/ts/controllers/` et les fichiers i18n.

Note les features identifiées — elles structurent les 5 documents.

## Étape 3 — Générer les 5 documents

Dossier de sortie : `<APP>/docs/specs/` — le créer si absent.

---

### 3.1 `BUSINESS_RULES.md`

**Sources selon le pattern :**
- Pattern A/C legacy : contrôleurs/services/directives AngularJS dans `public/ts/`
- Pattern B/C React : composants dans `frontend/src/features/`, hooks dans `frontend/src/hooks/`, stores Zustand dans `frontend/src/store/`

Pour chaque feature identifiée : validations, conditions d'affichage, calculs/transformations, comportements conditionnels, messages d'erreur.

```markdown
## Feature: [nom]
### Règle: [titre court]
- **Contexte**: quand / dans quel écran
- **Condition**: si [condition]
- **Action**: alors [comportement]
- **Source**: [chemin/fichier/ligne]
```

---

### 3.2 `DATA_MODEL.md`

**Sources selon le pattern :**
- Pattern A/C legacy : `src/main/resources/jsonschema/`, classes Java dans `src/main/java/`
- Pattern B/C React : `frontend/src/models/` (types TypeScript), `backend/src/main/resources/jsonschema/`, classes Java dans `backend/src/main/java/`

```markdown
## Entité: [NomEntité]
| Champ | Type | Obligatoire | Calculé | Description |
| ----- | ---- | ----------- | ------- | ----------- |
| id    | String | oui | non | Identifiant unique |

**Relations**: [...]
**Source**: [fichier]
```

---

### 3.3 `WORKFLOWS.md`

**Sources selon le pattern :**
- Pattern A/C legacy : contrôleurs AngularJS, routing, `ng-show`/`ng-hide`, états `$scope`
- Pattern B/C React : fichiers dans `frontend/src/routes/`, hooks de navigation, `frontend/src/features/*/` (flux utilisateur)
- Dans tous les cas : `view-src/` si présent (pont AngularJS→React)

Numéroter (`WF-01`, `WF-02`…).

```markdown
## WF-01 — [nom du flux]
**Déclencheur**: [action/événement]
**États**: `[initial]` → [transition] → `[suivant]`
**Étapes**: 1. … 2. …
**Cas d'erreur**: [...]
**Source**: [fichier]
```

Si `view-src/` ou `frontend/src/routes/redirections/` existe, documenter les redirections :

```markdown
## WF-XX — Redirections de rétrocompatibilité
| Route legacy (hash) | Route React cible | Paramètres | Source |
| ------------------- | ----------------- | ---------- | ------ |
| `#/view-<App>/:id`  | `/id/:id`         | `id`       | fichier:12 |
```

---

### 3.4 `PERMISSIONS.md`

**Sources selon le pattern :**
- Pattern A/C legacy : annotations Java (`@SecuredAction`, `@ResourceFilter`) dans `src/main/java/`, conditions AngularJS (`ng-if` liées aux droits)
- Pattern B/C React : annotations Java dans `backend/src/main/java/`, store des droits dans `frontend/src/store/rights/` si présent, guards dans `frontend/src/routes/`

```markdown
## Ressource: [nom]
| Action | Rôle requis | Condition | Source |
| ------ | ----------- | --------- | ------ |
| créer  | OWNER       | -         | ...    |

**Notes**: [comportements particuliers]
```

---

### 3.5 `API_CONTRACTS.md`

**Sources selon le pattern :**
- Pattern A/C legacy : contrôleurs Java dans `src/main/java/` (annotations `@Get`/`@Post`/`@Put`/`@Delete`), services AngularJS (`$http`)
- Pattern B/C React : contrôleurs Java dans `backend/src/main/java/`, services API dans `frontend/src/services/api/`, hooks TanStack Query dans `frontend/src/services/queries/`

```markdown
## [MÉTHODE] /chemin/endpoint
**Description**: [...]   **Auth requise**: oui/non
**Paramètres**: | Nom | Type | Où | Obligatoire | Description |
**Corps**: `{ "exemple": "payload" }`
**Réponse succès** (200/201): `{ "exemple": "réponse" }`
**Erreurs connues**: [codes + conditions]
**Source**: [classe Java + fichier frontend]
```

---

## Étape 4 — Intégrer les specs/captures existantes

Chercher dans `<APP>/docs/`, `<APP>/README.md`, et `<APP>/CHANGELOG.md` : enrichir les documents avec les règles corroborées et annoter `**Confirmé par**: [source]`.

## Étape 5 — Compte-rendu

Produire `<APP>/docs/compte-rendus/CR-extract-business-docs-<APP>-<date>.md` :

```markdown
# CR — Extraction documents métier : <APP>
Date : <date>
Pattern structurel : [A / B / C]

## Documents générés
| Fichier | Contenu |
|---------|---------|
| <APP>/docs/specs/BUSINESS_RULES.md | X features, Y règles |
| <APP>/docs/specs/DATA_MODEL.md | X entités |
| <APP>/docs/specs/WORKFLOWS.md | X workflows |
| <APP>/docs/specs/PERMISSIONS.md | X ressources |
| <APP>/docs/specs/API_CONTRACTS.md | X endpoints |

## Sources analysées
- [liste des chemins parcourus avec le pattern détecté]

## Points ⚠️ À confirmer
- [zones où la logique était ambiguë]
```

Afficher le même résumé dans la réponse.

## Points d'attention

- **Ne pas inventer** : logique pas claire → `⚠️ À confirmer`, jamais une déduction présentée comme un fait.
- **Références obligatoires** : chaque règle pointe vers son fichier source avec le chemin relatif depuis la racine du module.
- **Exhaustivité > concision** : mieux vaut documenter trop que laisser des règles implicites.
- Les fichiers **i18n** sont d'excellents indices pour repérer des features non évidentes.
- Pour les apps Pattern C (transitional), distinguer clairement les règles legacy des règles du nouveau React.
