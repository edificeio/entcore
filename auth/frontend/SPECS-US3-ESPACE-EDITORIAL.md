# Brief d'implémentation — WAYF v2 / US3 : Espace éditorial (message d'accueil)

> **Usage** : Ce fichier est un brief complet destiné à une instance Claude pour implémenter l'US3 du WAYF v2. Il consolide le ticket Jira, les specs fonctionnelles et les décisions d'architecture. Il s'appuie sur l'US1 (sélection du profil niveau 1) qui doit être livrée au préalable.

---

## 1. Contexte produit

Le **WAYF** (Where Are You From) est la page de sélection d'identité fédérée (SSO) d'Edifice. L'utilisateur y choisit son profil (enseignant, élève, parent…) avant d'être redirigé vers son fournisseur d'identité (SAML).

Les collectivités qui déploient Edifice ont besoin de **personnaliser le message d'accueil** affiché sur la WAYF (bienvenue, consignes, liens utiles…). Ce message est configurable depuis la console d'admin et servi via l'endpoint `/auth/configure/welcome`. C'est l'objet de cette US3 : afficher ce contenu dans la zone éditoriale (à gauche en desktop, en dessous en mobile) qui était restée vide / placeholder depuis l'US1.

**Objectifs du chantier v2 (rappel) :**
- Portage sur React (l'existant est en AngularJS)
- Template paramétrable par projet/collectivité
- Nouvelle charte graphique
- **Espace éditorial (message d'accueil)** ← scope de cette US
- Meilleure gestion des erreurs

**Livraison cible :** rentrée 2026.

---

## 2. Périmètre de l'US3

### Ce qui est dans le scope
- Récupérer le contenu éditorial via **GET `/auth/configure/welcome`** au chargement de la WAYF
- Afficher le contenu dans la **zone dédiée** de l'interface WAYF v2 (zone gauche en desktop, dessous en mobile)
- **Multilingue** : afficher la langue de l'utilisateur si disponible, **fallback sur le français**
- **Masquage silencieux** de la zone si l'API ne retourne aucun contenu utile
- **Robustesse** : gestion des erreurs API (timeout, 404, 5xx) sans bloquer le reste de la WAYF
- **Sanitization** du HTML retourné avant injection dans le DOM
- Zone en **lecture seule** (aucune interaction utilisateur attendue)

### Ce qui est hors scope (US suivantes)
- Édition / configuration du contenu côté admin (console d'admin déjà existante, non impactée par cette US)
- Checkbox "Mémoriser mon choix"
- Pied de page CGU
- Gestion des erreurs d'authentification
- Alertes système
- Niveau 2 des providers (US2)

---

## 3. Contrat d'API

### Endpoint

```
GET /auth/configure/welcome?allLanguages=allLanguages
```

### Format de réponse

Réponse JSON avec un flag `enabled` et une entrée HTML par langue :

```json
{
  "enabled": true,
  "fr": "<div>Bienvenue sur l'ENT de la collectivité…</div>",
  "en": "<div>Welcome to the collectivity's ENT…</div>",
  "es": ""
}
```

### Règles d'interprétation

| Condition | Comportement attendu |
|---|---|
| HTTP 200 + `enabled: true` + contenu non vide dans la langue courante | Afficher le contenu de la langue courante |
| HTTP 200 + `enabled: true` + contenu vide/absent dans la langue courante, mais `fr` rempli | **Fallback** sur le contenu `fr` |
| HTTP 200 + `enabled: false` | Zone éditoriale **masquée** |
| HTTP 200 + toutes les langues vides | Zone éditoriale **masquée** |
| HTTP 404 | Zone éditoriale **masquée**, aucune erreur remontée à l'utilisateur |
| Timeout / erreur réseau / 5xx | Zone éditoriale **masquée**, log console niveau `warn`, WAYF reste fonctionnelle |

> Règle centrale : **aucune erreur visible pour l'utilisateur final**. L'espace éditorial est un bonus, pas un bloquant.

---

## 4. Structure de la page

### Rappel layout US1

- **Zone gauche (desktop) / dessous (mobile)** : espace éditorial — `placeholder` vide depuis l'US1
- **Zone droite (desktop) / dessus (mobile)** : sélection du profil (US1) + sous-profil (US2)

### Espace éditorial — US3

Au chargement de la page :

```
┌────────────────────────────┐   ┌────────────────────────────┐
│                            │   │  Titre : "Choisissez…"     │
│   [ Contenu éditorial      │   │  ─────────────────────     │
│     HTML injecté et        │   │  [ Bouton Enseignant ]      │
│     sanitisé ]             │   │  [ Bouton Élève ]           │
│                            │   │  [ Bouton Parent ]          │
│                            │   │                            │
└────────────────────────────┘   └────────────────────────────┘
        Zone éditoriale                 Zone sélection
```

Si aucun contenu utilisable → la zone gauche est **absente du DOM** (ou `display: none` — à trancher selon le layout, voir section 9). Le reste de la page n'est pas déplacé ni dégradé.

---

## 5. Données techniques

### Appel réseau

Un seul appel `fetch` au chargement initial du composant `WayfPage`, côté client :

```typescript
// Pseudo-code
const res = await fetch('/auth/configure/welcome?allLanguages=allLanguages', {
  method: 'GET',
  credentials: 'include',
  signal: abortController.signal,
});
```

- **Timeout** : 5 secondes (à confirmer, voir section 9). Au-delà, on considère que l'appel a échoué et la zone est masquée.
- **AbortController** : annuler l'appel si le composant est démonté avant la réponse (évite les warnings React et les `setState` sur composant démonté).
- **Pas de cache applicatif** : laisser le navigateur et les caches HTTP standards faire leur travail. La conf change rarement mais un utilisateur qui revient après édition doit voir le nouveau contenu.

### Sélection de la langue

La langue courante est déterminée par le mécanisme i18n existant d'Edifice (même source que pour les clés i18n des providers — biblio `edifice-ts-client` ou équivalent).

Logique de résolution du contenu à afficher :

```typescript
function pickContent(response: WelcomeResponse, lang: string): string | null {
  if (!response.enabled) return null;
  const candidate = response[lang];
  if (candidate && candidate.trim().length > 0) return candidate;
  const fallback = response.fr;
  if (fallback && fallback.trim().length > 0) return fallback;
  return null; // masquer la zone
}
```

### Sanitization du HTML

Le contenu retourné est du **HTML libre saisi par un admin**. Il **doit** passer par une étape de sanitization avant d'être injecté dans le DOM pour éliminer tout risque XSS.

**Librairie recommandée** : `DOMPurify` (ou équivalent déjà présent dans le monorepo Edifice — à vérifier avant d'ajouter une dépendance).

```tsx
import DOMPurify from 'dompurify';

<aside
  className="wayf-welcome"
  dangerouslySetInnerHTML={{ __html: DOMPurify.sanitize(content) }}
/>
```

> ⚠️ **Ne jamais** injecter directement le HTML via `dangerouslySetInnerHTML` sans sanitization. C'est un point de review obligatoire lors du PR.

### State React

Trois états possibles pour la zone éditoriale, à modéliser avec un type discriminé :

```typescript
type WelcomeState =
  | { status: 'loading' }
  | { status: 'hidden' }          // enabled:false, contenu vide, ou erreur
  | { status: 'ready'; html: string };

const [welcome, setWelcome] = useState<WelcomeState>({ status: 'loading' });
```

- **`loading`** : pendant l'appel → ne rien afficher (ou un skeleton discret, à trancher avec le Design, voir section 9). **Important** : la zone de sélection (US1/US2) doit rester interactive pendant ce temps, l'appel `welcome` ne doit **pas bloquer** le rendu du reste.
- **`hidden`** : zone éditoriale absente / masquée.
- **`ready`** : contenu HTML sanitizé à injecter.

### Hook dédié

Encapsuler toute cette logique (fetch, sélection de langue, sanitization, gestion d'erreur, timeout, abort) dans un hook dédié `useWelcomeMessage()` plutôt que dans le composant. Facilite les tests unitaires et la réutilisation.

```typescript
// frontend/src/hooks/useWelcomeMessage.ts
export function useWelcomeMessage(): WelcomeState { /* ... */ }
```

---

## 6. Architecture technique

### Stack

Identique à l'US1 et US2 :
- **React** + TypeScript
- Composant dans le module `auth` du monorepo Edifice
- Nouvelle dépendance potentielle : `DOMPurify` (à vérifier si déjà présente dans le monorepo)

### Composants à créer / modifier

- **`WayfPage`** (existant, modifié) : appelle `useWelcomeMessage()` et passe le résultat à `WelcomeArea`
- **`WelcomeArea`** (nouveau) : reçoit un `WelcomeState`, rend le HTML sanitisé ou rien
- **`useWelcomeMessage`** (nouveau, hook) : fetch + timeout + abort + sélection de langue + sanitization

### Intégration i18n

L'hook lit la langue courante via le même mécanisme que le reste de la WAYF. Pas de nouvelle clé i18n à créer (le contenu éditorial est du HTML brut, pas des clés de traduction).

---

## 7. Info déploiement

- **Module à versionner** : `entcore`
- **Configuration de recette** : vérifier que l'endpoint `/auth/configure/welcome` est bien **exposé** et **renseigné** sur l'environnement de recette utilisé. À coordonner avec l'équipe ops/recette avant livraison.
  - Idéalement, que la recette contienne un contenu configuré dans **au moins deux langues** (FR + EN) pour valider le cas de fallback.
  - Prévoir également un scénario de test avec `enabled: false` et un scénario avec l'endpoint indisponible (404) pour valider la robustesse.
- Aucun changement côté console d'admin dans cette US.

---

## 8. Responsive design

Inchangé par rapport à l'US1 côté structure :

| Breakpoint | Comportement |
|---|---|
| Mobile (< 768px) | Une seule colonne — zone éditoriale **sous** la zone de sélection |
| Desktop (≥ 768px) | Deux colonnes côte à côte — zone éditoriale à gauche |

**Point d'attention spécifique US3** : le contenu HTML est saisi par un admin et peut contenir des éléments mal dimensionnés (images larges, tableaux, etc.). Prévoir des contraintes CSS sur la zone éditoriale :

```css
.wayf-welcome {
  overflow-wrap: break-word;
}
.wayf-welcome img,
.wayf-welcome table {
  max-width: 100%;
  height: auto;
}
```

Ne pas essayer de gérer tous les cas pathologiques — c'est la responsabilité de l'admin. Mais poser un garde-fou minimal.

---

## 9. Questions ouvertes

- **Timeout de l'appel welcome** : 5s proposé, à confirmer. Trop court → faux masquages sur connexions lentes. Trop long → dégrade la perception de chargement.
- **Affichage pendant le loading** : skeleton discret ou vraiment rien ? À clarifier avec le Design. Recommandation par défaut : rien, pour éviter un flash visuel court si l'appel aboutit vite.
- **Stratégie de masquage** : retirer du DOM ou `display: none` ? Impact sur le layout desktop à valider avec l'intégrateur.
- **Sanitization — niveau de permissivité** : DOMPurify avec config par défaut est strict (pas de `<script>`, pas d'attributs `on*`). Est-ce qu'on autorise des balises particulières que la console d'admin génère (`<iframe>` pour vidéos, par exemple) ? À auditer sur un échantillon de contenus existants côté collectivités avant de relaxer quoi que ce soit.

---

## 10. Liens et ressources

- Brief US1 (dépendance directe) : [[WAYF-v2-US1-Implementation-Brief]]
- Brief US2 : [[WAYF-v2-US2-Implementation-Brief]]
- Confluence — Processus d'intégration WAYF : https://edifice-community.atlassian.net/wiki/spaces/ODE/pages/4387438672/Processus+d+int+gration+de+la+WAYF
- Figma : https://www.figma.com/design/WZ6sK61pk08JLKEp5bjJF4/W---WAYF-mars-2024?node-id=561-7825&m=dev
- DOMPurify : https://github.com/cure53/DOMPurify

---

## 11. Risque / Test

| Cas de test | Résultat attendu | Résultat |
|---|---|---|
| Contenu disponible dans la langue de l'utilisateur | Contenu affiché dans la bonne langue | 🟢🟠🔴 |
| Contenu disponible uniquement en FR, utilisateur en EN | Contenu FR affiché en fallback | 🟢🟠🔴 |
| API retourne un contenu vide | Zone éditoriale masquée, reste de l'interface non impacté | 🟢🟠🔴 |
| API en erreur (timeout / 404) | Pas d'erreur bloquante, WAYF fonctionnel sans pub space | 🟢🟠🔴 |

### Cas de test complémentaires (robustesse et sécurité)

| Cas de test | Résultat attendu |
|---|---|
| Réponse HTTP 200 avec `enabled: false` | Zone masquée, aucune erreur, sélection fonctionne |
| Réponse HTTP 500 | Zone masquée, warning console, sélection fonctionne |
| Réponse avec toutes les langues vides | Zone masquée |
| Contenu HTML contenant `<script>alert(1)</script>` | Script **strippé** par la sanitization, pas d'exécution |
| Contenu HTML contenant `onerror=` sur une image | Attribut `onerror` **strippé** par la sanitization |
| Appel welcome très lent (> timeout) | Zone masquée après timeout, sélection reste interactive pendant toute la durée |
| Démontage du composant avant réponse | Pas de warning React "setState on unmounted component", abort effectif |
| Contenu avec image très large | Image contrainte par `max-width: 100%`, pas de débordement du layout |

---

## 12. Checklist de livraison US3

- [ ] Hook `useWelcomeMessage` avec fetch, timeout, abort, sélection de langue, sanitization
- [ ] Composant `WelcomeArea` qui consomme `WelcomeState` et rend le HTML sanitisé
- [ ] Intégration dans `WayfPage` (zone gauche desktop / dessous mobile)
- [ ] Sanitization via DOMPurify (ou équivalent monorepo) — **point de review obligatoire**
- [ ] Gestion des trois états : `loading`, `hidden`, `ready`
- [ ] Fallback FR quand la langue courante n'a pas de contenu
- [ ] Masquage silencieux sur `enabled: false`, contenu vide, 404, 5xx, timeout
- [ ] Contraintes CSS anti-débordement (`max-width: 100%` sur images/tableaux)
- [ ] Tests unitaires : sélection de langue + fallback, chaque branche d'erreur, timeout, abort
- [ ] Test manuel XSS avec un payload connu (`<script>`, `onerror=`) pour valider la sanitization
- [ ] Coordination recette : endpoint `/auth/configure/welcome` exposé et renseigné, scénarios FR+EN et `enabled:false` préparés
- [ ] Décisions tranchées sur les questions ouvertes (section 9) + implémentation conforme
