# Brief d'implémentation — WAYF v2 / US1 : Sélection du profil (Niveau 1)

> **Usage** : Ce fichier est un brief complet destiné à une instance Claude pour implémenter l'US1 du WAYF v2. Il consolide le ticket Jira, les specs fonctionnelles et les décisions d'architecture.

---

## 1. Contexte produit

Le **WAYF** (Where Are You From) est la page de sélection d'identité fédérée (SSO) d'Edifice. L'utilisateur y choisit son profil (enseignant, élève, parent…) avant d'être redirigé vers son fournisseur d'identité (SAML).

**Objectifs du chantier v2 :**

- Portage sur React (l'existant est en AngularJS)
- Template paramétrable par projet/collectivité
- Nouvelle charte graphique
- Espace éditorial (message d'accueil)
- Meilleure gestion des erreurs

**Livraison cible :** rentrée 2026.

---

## 2. Périmètre de l'US1

### Ce qui est dans le scope

- Afficher les providers de **niveau 1** issus de la configuration du domaine
- Cliquer sur un provider avec `acs` → redirect immédiate vers l'authentification
- Cliquer sur un provider avec `children` → **stub** : afficher un placeholder "Niveau 2 — à implémenter" (pas de fonctionnalité réelle)
- Chaque provider affiche son libellé via sa **clé i18n**
- Chaque provider est coloré via la configuration CSS
- Lecture de la configuration `wayf-v2`
- Résolution de l'i18n via les fichiers de traduction existants dans le thème courant
- **Gestion d'une conf par défaut** si rien n'est trouvé (la future WAYF EDIFICE générique)
- Validation du **format de configuration JSON** décrit ci-dessous

### Ce qui est hors scope (US suivantes)

- Niveau 2 (sélection du sous-niveau avec `children`)
- Checkbox "Mémoriser mon choix"
- Zone éditoriale gauche (message d'accueil via `/auth/configure/welcome`)
- Pied de page CGU
- Gestion des erreurs de connexion
- Alertes système

---

## 3. Format de configuration JSON (à valider dans cette US)

```json
{
  "wayf-v2": {
    "domaine.fr": {
      "providers": [
        {
          "i18n": "wayf.teacher",
          "acs": "/auth/saml/...",
          "color": "#c53030"
        },
        {
          "i18n": "wayf.student",
          "children": [
            {
              "i18n": "wayf.student.primary",
              "acs": "/auth/saml/..."
            },
            {
              "i18n": "wayf.student.secondary",
              "acs": "/auth/saml/..."
            }
          ]
        }
      ]
    }
  }
}
```

### Règles de la configuration

| Champ      | Type   | Obligatoire          | Description                                                                               |
| ---------- | ------ | -------------------- | ----------------------------------------------------------------------------------------- |
| `i18n`     | string | oui                  | Clé de traduction dans les fichiers i18n du thème                                         |
| `acs`      | string | si pas de `children` | URL SAML vers laquelle rediriger. Le `callBack` est appendé côté serveur.                 |
| `children` | array  | si pas de `acs`      | Liste de sous-providers (niveau 2)                                                        |
| `color`    | string | non                  | Couleur CSS du bouton (hex ou variable CSS). Peut aussi être géré par classe CSS `.{key}` |

Un provider a soit `acs`, soit `children`, jamais les deux.

### Configuration par défaut (fallback)

Si aucune configuration `wayf-v2` n'est trouvée pour le domaine courant, utiliser une configuration par défaut représentant la WAYF Edifice générique :

```json
{
  "providers": [
    { "i18n": "wayf.teacher", "acs": "/auth/saml/default-teacher" },
    { "i18n": "wayf.student", "acs": "/auth/saml/default-student" },
    { "i18n": "wayf.parent", "acs": "/auth/saml/default-parent" },
    { "i18n": "wayf.personnel", "acs": "/auth/saml/default-personnel" },
    { "i18n": "wayf.guest", "acs": "/auth/saml/default-guest" }
  ]
}
```

---

## 4. Structure de la page

### Layout global

- **Zone gauche** : espace éditorial (pub space) — hors scope US1, afficher un placeholder vide ou une image de fond
- **Zone droite** : sélection du profil

### Assets thème

Logo et background accessibles depuis :

```
/assets/themes/${childTheme}/img/logo.png
/assets/themes/${childTheme}/img/background.png
```

### Zone droite — Niveau 1 (scope US1)

```
Titre : clé i18n "wayf.select.profile"
────────────────────────────────────
[ Bouton Provider 1 (libellé i18n) ]
[ Bouton Provider 2 (libellé i18n) ]
[ Bouton Provider 3 (libellé i18n) ]
...ordre selon la conf
────────────────────────────────────
```

- Liste verticale de boutons pleine largeur
- Dans l'ordre de la configuration
- Couleur appliquée via CSS : classe `.{clé-i18n-sans-points}` ou couleur inline depuis `color` dans la conf
- Au clic sur un provider avec `acs` → `window.location.href = acs`
- Au clic sur un provider avec `children` → afficher stub niveau 2

### Stub niveau 2 (scope US1 — minimal)

```
[ ← Retour ]
[ Niveau 2 en cours d'implémentation ]
```

> La clé i18n `wayf.select.level` n'est pas encore créée — ne pas l'utiliser dans cette US. Un texte en dur ou absent suffit pour le stub.

---

## 5. Données techniques

### Lecture de la configuration

La configuration est **locale au frontend** dans cette phase. Elle est stockée dans :

```
frontend/src/config/wayf.config.ts
```

Ce fichier exporte la conf `wayf-v2` indexée par domaine. La logique de lookup : trouver l'entrée correspondant à `window.location.hostname`, sinon retourner la conf par défaut.

```typescript
// frontend/src/config/wayf.config.ts
export const wayfConfig: WayfConfig = {
  "wayf-v2": {
    "domaine.fr": {
      "providers": [ ... ]
    }
  }
};

export const DEFAULT_WAYF_CONFIG: WayfDomainConfig = {
  "providers": [
    { "i18n": "wayf.teacher", "acs": "/auth/saml/default-teacher" },
    { "i18n": "wayf.student", "acs": "/auth/saml/default-student" },
    { "i18n": "wayf.parent", "acs": "/auth/saml/default-parent" },
    { "i18n": "wayf.personnel", "acs": "/auth/saml/default-personnel" },
    { "i18n": "wayf.guest", "acs": "/auth/saml/default-guest" }
  ]
};
```

### Activation de la v2

✅ **Traité dans l'US précédente (v0).** La bascule se fait via le cookie `wayf-beta` : si ce cookie est présent, la nouvelle WAYF React s'affiche. Rien à implémenter dans cette US.

### i18n

Les fichiers de traduction sont dans le thème courant :

```
/assets/themes/${childTheme}/i18n/fr.json
/assets/themes/${childTheme}/i18n/en.json
...
```

Utiliser le mécanisme i18n existant d'Edifice (bibliothèque `edifice-ts-client` ou équivalent). Les clés à résoudre sont les valeurs du champ `i18n` dans la conf.

**Clés i18n existantes à utiliser dans cette US :**

- `wayf.select.profile` — titre de la page niveau 1
- `wayf.teacher`, `wayf.student`, `wayf.parent`, etc. — libellés des providers

> ⚠️ Ne pas créer de nouvelles clés i18n dans cette US. Les clés `wayf.select.level`, `wayf.remember`, `wayf.label.cgu` seront traitées dans les US suivantes.

### Couleurs via CSS

Option 1 — Classes CSS dans le thème :

```css
/* Dans le thème */
.wayf-btn.wayf-teacher {
  background-color: #c53030;
}
.wayf-btn.wayf-student {
  background-color: #2b6cb0;
}
```

Option 2 — Couleur inline depuis la conf (champ `color`) :

```tsx
<button style={{ backgroundColor: provider.color }}>{t(provider.i18n)}</button>
```

**Décision** : supporter les deux en parallèle — appliquer la couleur inline si `color` est défini dans la conf, et toujours poser la classe CSS (`.wayf-btn.wayf-{key}`) pour permettre la surcharge par le thème. Le tri final entre les deux approches sera fait dans une US ultérieure.

### callBack

Le paramètre `callBack` est appendé **côté serveur** à chaque ACS à chaque requête (non mis en cache). Le frontend n'a pas à le gérer — il redirige simplement vers l'`acs` reçu dans la conf.

---

## 6. Architecture technique

### Stack

- **React** (portage depuis AngularJS)
- TypeScript
- Le composant vit dans le module `auth` du monorepo Edifice (à confirmer)

### Double build

✅ **Traité dans l'US précédente (v0).** Le double build (ancienne WAYF AngularJS + nouvelle WAYF React dans le module `auth`) est déjà en place. Cette US s'appuie sur cette infrastructure existante.

### Références de code existantes

- WAYF HDF : https://github.com/edificeio/sites-publics-hdf (React, peut servir de référence)
- WAYF Normandie : https://github.com/edificeio/wayf-normandie
- Exemple live HDF : https://connexion.enthdf.fr/
- Exemple live classique : https://nati.pf/auth/saml/wayf

### Extrait de référence — récupération du message d'accueil (hors scope US1, pour info)

```typescript
// Endpoint welcome message (US suivante)
fetch('/auth/configure/welcome?allLanguages=allLanguages');
// Retourne : { enabled: true, fr: "<div>...</div>", en: "", ... }
```

---

## 7. Maquettes

Les maquettes sont disponibles sur Figma :
[WAYF v2 — Figma](https://www.figma.com/design/WZ6sK61pk08JLKEp5bjJF4/W---WAYF-mars-2024?node-id=561-7825&m=dev)

**Description visuelle niveau 1 :**

- Page en deux colonnes (gauche : image/édito, droite : formulaire de choix)
- Zone droite : titre + liste de boutons colorés pleine largeur
- Chaque bouton occupe toute la largeur avec un fond coloré selon le profil
- Design sobre, cohérent avec le Design System Edifice

---

## 8. Responsive design

L'application doit être **entièrement responsive**. C'est l'une des motivations du chantier v2 (l'existant avait des problèmes d'affichage sur mobile).

### Comportement attendu par breakpoint

Deux breakpoints uniquement :

| Breakpoint        | Comportement                                                                   |
| ----------------- | ------------------------------------------------------------------------------ |
| Mobile (< 768px)  | Une seule colonne — sélection du profil en premier, zone éditoriale en dessous |
| Desktop (≥ 768px) | Deux colonnes côte à côte (gauche édito + droite sélection)                    |

### Points d'attention

- Les boutons providers doivent rester facilement cliquables sur tactile (hauteur minimale recommandée : 48px)
- Le layout deux colonnes se replie en colonne unique sur mobile
- Les assets thème (logo, background) doivent s'adapter sans débordement

---

## 9. Questions ouvertes (à trancher avant / pendant l'implémentation)

Aucune question ouverte bloquante — toutes les décisions sont prises pour cette US.

---

## 10. Liens et ressources

- Confluence — Processus d'intégration WAYF : https://edifice-community.atlassian.net/wiki/spaces/ODE/pages/4387438672/Processus+d+int+gration+de+la+WAYF
- Figma : https://www.figma.com/design/WZ6sK61pk08JLKEp5bjJF4/W---WAYF-mars-2024?node-id=561-7825&m=dev
- GitHub HDF : https://github.com/edificeio/sites-publics-hdf
- GitHub Normandie : https://github.com/edificeio/wayf-normandie

---

## 11. Checklist de livraison US1

- [ ] Composant React `WayfPage` avec layout deux colonnes
- [ ] Composant `ProviderList` qui rend la liste des providers niveau 1
- [ ] Composant `ProviderButton` avec libellé i18n + couleur
- [ ] Lecture et parsing de la conf `wayf-v2` (avec fallback sur conf par défaut)
- [ ] Redirect vers `acs` au clic sur un provider terminal
- [ ] Stub niveau 2 au clic sur un provider avec `children`
- [ ] Tests unitaires sur la logique de parsing de conf et fallback
- [ ] Storybook / démo isolée du composant
- [ ] Format JSON de conf documenté et validé avec l'équipe
- [ ] Responsive : layout une colonne sur mobile, deux colonnes sur desktop
- [ ] Vérification affichage sur mobile (Chrome DevTools + device réel si possible)
