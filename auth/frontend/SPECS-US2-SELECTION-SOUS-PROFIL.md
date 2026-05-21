# Brief d'implémentation — WAYF v2 / US2 : Sélection du sous-profil (Niveau 2)

> **Usage** : Ce fichier est un brief complet destiné à une instance Claude pour implémenter l'US2 du WAYF v2. Il consolide le ticket Jira, les specs fonctionnelles et les décisions d'architecture. Il s'appuie sur l'US1 (sélection du profil niveau 1) qui doit être livrée au préalable.

---

## 1. Contexte produit

Le **WAYF** (Where Are You From) est la page de sélection d'identité fédérée (SSO) d'Edifice. L'utilisateur y choisit son profil (enseignant, élève, parent…) avant d'être redirigé vers son fournisseur d'identité (SAML).

Certains profils nécessitent un second niveau de sélection (par exemple : "Élève" → "Primaire" / "Secondaire"). C'est l'objet de cette US2 : remplacer le stub posé en US1 par une vraie sélection de niveau 2.

**Objectifs du chantier v2 (rappel) :**
- Portage sur React (l'existant est en AngularJS)
- Template paramétrable par projet/collectivité
- Nouvelle charte graphique
- Espace éditorial (message d'accueil)
- Meilleure gestion des erreurs

**Livraison cible :** rentrée 2026.

---

## 2. Périmètre de l'US2

### Ce qui est dans le scope
- Afficher les **`children`** du provider sélectionné au niveau 1
- Chaque enfant **hérite de la couleur** du provider parent
- Cliquer sur un enfant redirige vers son **`acs`**
- Permettre le **retour au niveau 1** sans perte de contexte (la liste complète doit se réafficher sans rechargement)
- Les libellés des enfants sont résolus via leur **clé i18n**
- Remplacement du stub "Niveau 2 — à implémenter" posé en US1

### Ce qui est hors scope (US suivantes)
- Checkbox "Mémoriser mon choix"
- Zone éditoriale gauche (message d'accueil via `/auth/configure/welcome`)
- Pied de page CGU
- Gestion des erreurs de connexion
- Alertes système
- Niveaux de profondeur supérieurs à 2 (non supporté par design)

---

## 3. Format de configuration JSON (rappel US1)

Le format est **déjà validé en US1**. Extrait de la partie qui concerne cette US :

```json
{
  "wayf-v2": {
    "domaine.fr": {
      "providers": [
        {
          "i18n": "wayf.student",
          "color": "#2b6cb0",
          "children": [
            {
              "i18n": "wayf.student.primary",
              "acs": "/auth/saml/student-primary"
            },
            {
              "i18n": "wayf.student.secondary",
              "acs": "/auth/saml/student-secondary"
            }
          ]
        }
      ]
    }
  }
}
```

### Règles spécifiques au niveau 2

| Champ | Type | Obligatoire | Description |
|---|---|---|---|
| `i18n` | string | oui | Clé de traduction du libellé de l'enfant |
| `acs` | string | oui | URL SAML de redirection (obligatoire sur un enfant — un enfant est toujours terminal) |
| `children` | — | **interdit** | Un enfant ne peut **pas** lui-même avoir des `children`. Structure limitée à 2 niveaux. |
| `color` | — | — | **Ignoré** au niveau 2. La couleur est héritée du parent. |

> ⚠️ Un enfant qui contient lui-même un champ `children` est une **configuration invalide**. Le parser doit au minimum logger un warning ; idéalement ignorer le sous-arbre. À trancher avec l'équipe (voir section 9).

---

## 4. Structure de la page

### Rappel niveau 1 (livré en US1)

```
Titre : clé i18n "wayf.select.profile"
────────────────────────────────────
[ Bouton Provider 1 (libellé i18n) ]
[ Bouton Provider 2 (libellé i18n, avec children) ]
[ Bouton Provider 3 (libellé i18n) ]
────────────────────────────────────
```

### Niveau 2 (scope US2)

Au clic sur un provider de niveau 1 qui contient des `children`, la zone droite **remplace** la liste niveau 1 par :

```
[ ← Retour ]           ← bouton retour niveau 1
Titre : clé i18n "wayf.select.level"   ← à créer dans cette US
────────────────────────────────────
[ Bouton Enfant 1 (libellé i18n) ]     ← couleur du parent
[ Bouton Enfant 2 (libellé i18n) ]     ← couleur du parent
...ordre selon la conf
────────────────────────────────────
```

- Liste verticale de boutons pleine largeur (même rendu visuel que niveau 1)
- Dans l'ordre de la configuration
- **Couleur = couleur du provider parent** (voir section 5 "Héritage de couleur")
- Au clic sur un enfant → `window.location.href = child.acs`
- Au clic sur ← Retour → revenir à la liste niveau 1, **sans rechargement ni perte de contexte**

### Clés i18n à créer dans cette US

- `wayf.select.level` — titre de la page niveau 2 (non créée en US1, volontairement)

---

## 5. Données techniques

### Lecture des children

La configuration est **déjà chargée côté frontend en US1** (via `frontend/src/config/wayf.config.ts`).

**Règle** : aucun nouvel appel réseau pour récupérer les `children`. Ils sont déjà présents dans l'objet `provider.children` du niveau 1.

### Navigation niveau 1 ↔ niveau 2

La navigation est gérée **en state React local**, pas via le router. Cela évite :
- Un changement d'URL intermédiaire qui compliquerait le retour arrière navigateur
- Un rechargement de la conf ou de la page
- La création d'une route dédiée pour un état transitoire

**Shape du state** (à placer dans le composant `WayfPage` ou un hook dédié `useWayfNavigation`) :

```typescript
type WayfView =
  | { level: 1 }
  | { level: 2; parent: WayfProvider };

const [view, setView] = useState<WayfView>({ level: 1 });
```

- Clic sur un provider avec `children` → `setView({ level: 2, parent: provider })`
- Clic sur ← Retour → `setView({ level: 1 })`
- Clic sur un enfant (toujours terminal) → `window.location.href = child.acs`

### Héritage de couleur

La couleur du parent est propagée aux boutons enfants. Deux approches, **à combiner comme en US1** :

**1. Classe CSS** : la classe `.wayf-provider-btn--{cssKey-parent}` appliquée au bouton parent est **aussi appliquée** aux boutons enfants.

```tsx
// Au niveau 2, la classe et la couleur proviennent du parent, pas de l'enfant
<ProviderButton
  provider={{ ...child, color: parent.color }}
  onClick={...}
/>
```

**2. Couleur inline** : si `parent.color` est défini, l'appliquer aux boutons enfants via le champ `color` passé en prop (mécanisme existant de `ProviderButton`).

> ⚠️ Le champ `color` d'un enfant — s'il existait par erreur — doit être **ignoré**. La source de vérité est le parent.

### Réutilisation de `ProviderButton`

`ProviderButton` est **réutilisé tel quel** au niveau 2 : on lui passe un provider enfant enrichi avec la couleur du parent :

```tsx
const childWithParentColor: WayfProvider = {
  ...child,
  color: parent.color,
};
<ProviderButton provider={childWithParentColor} onClick={handleChildClick} />
```

### i18n

Mécanisme identique à l'US1. Nouvelles clés à ajouter dans `src/main/resources/i18n/fr.json` :
- `wayf.select.level` — FR + EN minimum

Les clés des enfants (`wayf.student.primary`, `wayf.student.secondary`, etc.) doivent être présentes dans les fichiers de traduction du thème utilisé en recette.

### callBack

Inchangé par rapport à l'US1 : le `callBack` est appendé **côté serveur** à l'ACS, le frontend redirige simplement vers l'`acs` reçu dans la conf.

---

## 6. Architecture technique

### Stack

Identique à l'US1 :
- **React** + TypeScript
- Composant dans le module `auth` du monorepo Edifice
- Pas de nouvelle dépendance introduite par l'US2

### Composants à créer / modifier

| Composant | Action | Détail |
|---|---|---|
| `WayfPage` | **modifier** | Remplacer le `useState<WayfProvider \| null>` par `useState<WayfView>({ level: 1 })` |
| `ChildrenList` | **créer** | `src/components/ChildrenList/` — prend `parent: WayfProvider` + `onBack: () => void` |
| `ProviderButton` | inchangé | Réutilisé au niveau 2 avec couleur héritée du parent |
| `Level2Stub` | **supprimer** | Remplacé par `ChildrenList` |

### Fichiers à modifier

| Fichier | Changement |
|---|---|
| `src/routes/pages/Wayf.tsx` | Remplacer `selectedProvider` par `WayfView`, brancher `ChildrenList` |
| `src/components/index.ts` | Ajouter export `ChildrenList`, retirer `Level2Stub` |
| `src/main/resources/i18n/fr.json` | Ajouter `wayf.select.level` |

---

## 7. Info déploiement

- **Module à versionner** : `entcore`
- **Configuration de recette** : s'assurer que la configuration utilisée en recette contient **au moins un provider avec `children`** pour pouvoir valider le scénario complet de bout en bout.
- Les clés i18n `wayf.student.primary`, `wayf.student.secondary` (ou équivalents selon le thème de recette) doivent être présentes dans les fichiers de traduction déployés.

---

## 8. Responsive design

Inchangé par rapport à l'US1. Rappel :

| Breakpoint | Comportement |
|---|---|
| Mobile (< 768px) | Une seule colonne — la liste niveau 2 remplace la liste niveau 1 au même endroit |
| Desktop (≥ 768px) | Deux colonnes — la zone droite alterne entre niveau 1 et niveau 2 |

**Point d'attention spécifique niveau 2** : le bouton "← Retour" doit rester facilement cliquable sur tactile (hauteur minimale 48px, zone de clic confortable).

---

## 9. Questions ouvertes

- **Validation de conf** : que faire si un enfant contient lui-même un `children` (config invalide) ? Logger + ignorer, ou lever une erreur bloquante ? À trancher avec l'équipe avant merge.
- **Animation de transition** niveau 1 ↔ niveau 2 : attendue ou pas ? Absente des maquettes à date. À clarifier avec le Design.

---

## 10. Liens et ressources

- Brief US1 (dépendance directe) : `SPECS-US1-SELECTION-PROFIL.md`
- Confluence — Processus d'intégration WAYF : https://edifice-community.atlassian.net/wiki/spaces/ODE/pages/4387438672/Processus+d+int+gration+de+la+WAYF
- Figma : https://www.figma.com/design/WZ6sK61pk08JLKEp5bjJF4/W---WAYF-mars-2024?node-id=561-7825&m=dev
- GitHub HDF : https://github.com/edificeio/sites-publics-hdf
- GitHub Normandie : https://github.com/edificeio/wayf-normandie

---

## 11. Risque / Test

| Cas de test | Résultat attendu | Résultat |
|---|---|---|
| Provider avec children sélectionné au niveau 1 | Affichage du niveau 2 avec les enfants correspondants | 🟢🟠🔴 |
| Couleur des boutons enfants | Identique à la couleur du provider parent | 🟢🟠🔴 |
| Clic sur un enfant | Redirection vers l'`acs` de l'enfant | 🟢🟠🔴 |
| Bouton retour niveau 1 | Retour à la liste complète des providers sans rechargement | 🟢🟠🔴 |

### Cas de test complémentaires (non-régression)

| Cas de test | Résultat attendu |
|---|---|
| Provider sans children (terminal) au niveau 1 | Comportement US1 inchangé : redirect direct vers `acs` |
| Aller-retour niveau 1 → niveau 2 → niveau 1 → niveau 2 (autre parent) | Les enfants affichés correspondent bien au dernier parent sélectionné |
| Enfant avec champ `color` défini | Le `color` de l'enfant est ignoré, la couleur du parent est appliquée |
| Configuration invalide (enfant avec children) | Warning/erreur conforme à la décision de la section 9 — pas de crash |

---

## 12. Checklist de livraison US2

- [ ] State `WayfView` (niveau 1 / niveau 2) dans `WayfPage`
- [ ] Composant `ChildrenList` avec rendu des enfants + bouton retour
- [ ] Réutilisation de `ProviderButton` au niveau 2 avec héritage de couleur (`color` du parent propagé)
- [ ] Suppression du composant `Level2Stub` de l'US1
- [ ] Ajout de la clé i18n `wayf.select.level` (FR + EN)
- [ ] Tests unitaires : navigation niveau 1 ↔ niveau 2, héritage de couleur, redirect enfant
- [ ] Storybook : story avec un provider contenant `children`
- [ ] Vérification responsive mobile / desktop
- [ ] Coordination recette : conf de recette contient au moins un provider avec `children`
- [ ] Décision tranchée sur la validation de conf (section 9) + implémentation conforme
