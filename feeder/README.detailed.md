# feeder — fonctionnement détaillé

> Version rapide : [README.md](README.md). Référence dense pour un assistant IA : [CLAUDE.md](CLAUDE.md).

## À quoi sert ce module ?

`feeder` est le module qui alimente l'annuaire Edifice (le graphe Neo4j : structures, classes, groupes,
utilisateurs) à partir de sources externes, et qui peut aussi ré-exporter ce même annuaire vers un
système national. Concrètement, il gère :

- **L'import AAF** (2nd degré — collèges/lycées) : le format d'échange "Siecle" utilisé par
  l'Éducation Nationale française pour les établissements secondaires.
- **L'import AAF1D** (1er degré — écoles primaires) : la même famille de format, mais pour les écoles
  élémentaires/maternelles, avec un vocabulaire et quelques champs différents (cantine, garderie...).
- **L'import CSV** : la voie manuelle/générique, utilisée quand il n'y a pas de flux académique
  automatique — un établissement (ou son administrateur local) dépose des fichiers `.csv` via un
  assistant web.
- **L'import de plannings** EDT/Pronote et UDT : les emplois du temps, stockés à part (MongoDB) car ce
  sont des données très différentes (cours, créneaux horaires) des données d'annuaire.
- **L'export Éliot** : le sens inverse de l'import AAF — on relit le graphe et on reproduit un fichier au
  même format XML, à destination du système ministériel Éliot, pour que les comptes créés localement
  dans l'ENT (invités, administrateurs locaux...) remontent aussi côté national.

Ce module ne fait aucune interface utilisateur et n'expose aucune route HTTP : tout se pilote depuis le
module `directory`, qui envoie des messages sur le bus d'événements de Vert.x. `feeder` est un pur
"worker" en arrière-plan.

## Vue d'ensemble de l'architecture

```
                         ┌─────────────────────────┐
  HTTP (module           │  entcore.feeder          │        Neo4j (graphe annuaire)
  "directory")  ───msg──▶│  Feeder (event-bus)      │───────▶ Structure / User / Class / Group / ...
                         │  action: import/export/…│
                         └───────────┬─────────────┘
                                     │ dispatch par "feeder": AAF|AAF1D|CSV|PRONOTE
                       ┌─────────────┼──────────────┬───────────────┐
                       ▼             ▼              ▼               ▼
                  AafFeeder    Aaf1dFeeder      CsvFeeder    EDTFeederLauncher
                       │             │              │               │
                       └─────────────┴──────────────┴───────────────┘
                                     │  (tous produisent le même JsonObject générique)
                                     ▼
                         dictionary/structures/Importer  (moteur générique, singleton)
                                     │
                                     ▼
                              TransactionHelper (batch Neo4j, 1000 statements)
```

Le point important : **quel que soit le format d'entrée, tout finit par appeler les mêmes méthodes**
(`Importer.createOrUpdateStructure`, `createOrUpdateStudent`, `createOrUpdatePersonnel`,
`createOrUpdateUser`, `createOrUpdateGuest`...). Chaque format n'est qu'une "façade" qui sait lire son
propre format de fichier et le traduire vers un `JsonObject` avec un ensemble fixe de noms de champs
(`externalId`, `structures`, `classes`, `groups`, `relative`, `functions`...). Comprendre ce moteur
générique une bonne fois pour toutes permet ensuite de comprendre n'importe quel format source en
quelques minutes.

## Comment un import est déclenché

Il n'y a **aucune route HTTP dans ce module**. Les routes vivent dans `entcore/directory` (par exemple
`ImportController` pour le CSV, `TimetableController` pour EDT/UDT, ou l'endpoint `/export`), et elles se
contentent d'envoyer un message JSON sur l'adresse de bus `entcore.feeder` avec un champ `"action"`. Le
verticle `Feeder` (`Feeder.java`) écoute cette adresse, et un grand `switch` sur l'action (méthode
`handleAction`) route vers le bon comportement :

- `"import"` (le cas le plus courant) → valide d'abord toutes les données à importer, et **n'importe
  réellement que si la validation ne remonte aucune erreur bloquante**. Si l'import réussit et que la
  configuration le demande, un export automatique peut être planifié un peu plus tard.
- `"validate"` → fait juste la validation, sans jamais écrire dans Neo4j (utile pour l'assistant CSV, qui
  affiche les erreurs à l'utilisateur avant qu'il ne confirme).
- `"export"` → déclenche l'export Éliot.
- `"transition"` → une opération de maintenance qui rejoue toute la structure d'un établissement (rare,
  utilisé pour des migrations/réorganisations).
- Une quarantaine d'actions `"manual-*"` → des opérations de CRUD manuel (créer une classe à la main,
  rattacher un utilisateur, fusionner deux comptes, etc.), qui ne passent pas par un fichier mais par
  `ManualFeeder`.
- Des actions liées aux doublons (`list-duplicate`, `merge-duplicate`, `ignore-duplicate`...) et à la
  suppression progressive des comptes (`delete-users`, `pre-delete-users`).

Comment le module sait-il s'il doit traiter le message comme de l'AAF, de l'AAF1D, du CSV ou du
Pronote ? Le message porte un champ `"feeder"` (par exemple `"feeder": "AAF"`), et `Feeder` maintient une
simple table `Map<String, Feed>` associant ce nom à une implémentation Java. Ajouter un nouveau format
d'import consiste, pour l'essentiel, à écrire une nouvelle implémentation de l'interface `Feed` et à
l'enregistrer dans cette table — tout le reste (validation, écriture Neo4j, rapport, gestion des
doublons, cycle de suppression) est déjà fourni par le moteur générique.

Deux façons de déclencher un import sans passer par une action HTTP explicite : une tâche planifiée
(cron), configurée par établissement ou globalement, qui envoie elle-même le message `"import"` à
intervalle régulier ; ou, pour le CSV, un dépôt de fichier zip dans un dossier surveillé.

## Le moteur générique : `Importer`

C'est la pièce centrale du module (`dictionary/structures/Importer.java`). Quelques points essentiels à
avoir en tête :

**C'est un singleton, et un seul import peut être actif à un instant donné, toutes sources
confondues.** Si un import AAF est en cours et qu'un import CSV arrive en même temps, le second est mis
en attente (dans une file interne) jusqu'à ce que le premier se termine. Ce n'est pas une limitation par
établissement : c'est une limitation globale à toute la plateforme.

**Au démarrage d'un run**, `Importer` précharge en mémoire (dans des caches statiques, partagés avec la
classe `GraphData`) l'intégralité des structures, profils, classes, groupes et fonctions déjà présents
dans Neo4j. Ça permet, ensuite, de savoir "cette classe existe-t-elle déjà ?" sans faire un aller-retour
Neo4j à chaque élève traité — une simple vérification en mémoire suffit, et le `MERGE` Neo4j n'est
déclenché que la toute première fois qu'un nouvel identifiant de classe/groupe apparaît.

**Chaque enregistrement passe par une validation JSON-schema** (les fichiers `dictionary/schema/*.json`
définissent quels champs sont obligatoires, quel format ils doivent respecter, et ce qui est
modifiable après création). Si la validation échoue pour un enregistrement (par exemple, un élève sans
date de naissance), cet enregistrement précis est simplement ignoré et noté dans le rapport final — le
reste de l'import continue normalement. Il n'y a pas d'exception qui remonte et interrompt tout.

**Les écritures Neo4j sont groupées par lots de 1000** statements avant d'être envoyées au serveur, à
l'intérieur d'une transaction serveur ouverte au premier envoi. Un `commit()` clôt la transaction ; la
méthode `persist()` d'`Importer` fait un `commit()` suivi immédiatement de la réouverture d'une nouvelle
transaction — c'est le mécanisme de "checkpoint" qui évite qu'un import massif ne fasse grossir une seule
transaction Neo4j à l'infini.

**Chaque enregistrement écrit porte une empreinte (checksum)** calculée sur ses champs validés : si un
élève réimporté n'a strictement pas changé depuis la dernière fois, aucune écriture Neo4j n'est
déclenchée pour lui — seul un changement réel coûte une écriture.

**À la fin d'un run**, `Importer` compare la liste des identifiants externes vus pendant ce run avec ceux
déjà présents en base pour cette même source. Tout compte présent en base mais absent du run courant est
marqué comme "disparu" (une date de disparition est posée sur le nœud), sans être supprimé
immédiatement — c'est le début d'un cycle de suppression progressive :

1. **Disparition** (fin de chaque import) : le compte reste utilisable, juste marqué.
2. **Pré-suppression** (tâche planifiée, ~90 jours plus tard par défaut) : le compte perd effectivement
   ses accès (ses appartenances à des groupes/classes sont supprimées), mais le nœud existe toujours — on
   peut encore le restaurer.
3. **Suppression définitive** (encore plus tard) : le nœud est réellement supprimé.

Un compte qui réapparaît dans un import ultérieur avant l'étape finale voit ces marques retirées
automatiquement.

## La détection des doublons

Quand deux formats d'import différents (par exemple AAF et CSV) créent chacun un compte pour la même
personne réelle (un enseignant qui existe à la fois dans le flux académique et qui a été ajouté à la
main), le module tente de détecter ces doublons : recherche floue sur le nom/prénom, comparaison de la
date de naissance et de l'email, calcul d'un score. Au-delà d'un certain score, une relation "doublon"
est posée entre les deux comptes en base, et un administrateur (ou une règle de fusion automatique, selon
la configuration) peut ensuite fusionner les deux comptes en un seul, en général en gardant celui qui
vient de la source la plus fiable (un compte AAF prime généralement sur un compte CSV, par exemple).

## Les formats source, un par un

### AAF — 2nd degré (collèges, lycées)

C'est le format historique et le plus riche. Le principe général : des fichiers XML nommés selon une
convention stricte (`..._EtabEducNat_0001.xml` pour les établissements, `..._Eleve_0001.xml` pour les
élèves, `..._PersEducNat_0001.xml` pour les enseignants et personnels, `..._PersRelEleve_0001.xml` pour
les responsables légaux/parents), traités dans cet ordre précis (les structures doivent exister avant que
les élèves puissent s'y rattacher, etc.).

Une particularité importante à retenir : **les classes et les groupes n'existent jamais comme entité XML
à part**. Un élève ou un enseignant porte simplement, dans un de ses champs, une valeur du type
`<identifiantEtablissement>$<nomDeClasse>` — et c'est la première fois que le module rencontre ce couple
qu'il crée réellement la classe. Il n'y a jamais de fichier "Classe.xml" à fournir.

De la même façon, les liens vers les parents/responsables légaux sont encodés dans une chaîne de
caractères compacte séparée par des `$` (un identifiant de parent suivi de plusieurs codes), et un parent
n'est réellement créé en base **que s'il est effectivement référencé par au moins un élève** — un fichier
de parents qui contiendrait des entrées orphelines les ignore silencieusement.

### AAF1D — 1er degré (écoles primaires)

Même famille de format, mêmes noms de fichiers, mais un dictionnaire de correspondance de champs
différent, et quelques différences de fond :

- Pas de notion de "matière enseignée"/parcours (MEF) : l'école primaire n'a pas cette structuration en
  disciplines/filières propre au secondaire.
- L'encodage des classes/groupes dans les champs élève est légèrement différent (plus de segments
  séparés par `$`, et la clé se construit avec un préfixe d'académie partagé plutôt que par
  établissement) — un détail à connaître si on veut écrire un outil (générateur de test, par exemple) qui
  couvre les deux formats.
- Des champs propres au primaire apparaissent (garderie du matin/soir, restaurant scolaire, études
  surveillées) qui n'ont pas d'équivalent côté secondaire.
- Un mécanisme spécifique permet de fusionner les informations d'un même enseignant qui interviendrait à
  la fois en primaire et en secondaire (contrôlé par une option de configuration).

### CSV

La voie "manuelle" : un établissement (ou son administrateur) dépose un fichier CSV par type de compte
(`Student.csv`, `Teacher.csv`, `Personnel.csv`, `Relative.csv`, `Guest.csv`), avec un en-tête de colonnes
que le module sait reconnaître (y compris en français, avec ou sans accents). Contrairement à l'AAF, le
CSV permet de créer des comptes "Invité" (Guest) — une catégorie qui n'existe simplement pas côté AAF.

Il y a deux passes bien distinctes : une passe de **validation** (qui ne touche jamais réellement à la
base, juste des lectures pour vérifier les doublons/existants) que l'assistant web utilise pour afficher
les erreurs avant que l'administrateur ne confirme, et une passe d'**import** qui écrit réellement. Fait
important : même quand on déclenche l'import directement (sans passer par l'étape "prévisualiser"), le
module relance systématiquement la validation en interne juste avant, et n'importe que si elle ne
remonte aucune erreur bloquante.

Une ligne de CSV en erreur ne bloque pas les autres : elle est simplement ignorée et notée dans le
rapport, et le reste du fichier continue d'être traité normalement. Seule une erreur au niveau du fichier
entier (fichier illisible, mal formé) interrompt la suite du traitement.

### Export Éliot

C'est le sens inverse : au lieu de lire un fichier XML pour peupler le graphe, ce module relit le graphe
Neo4j et régénère un fichier XML dans le même format, à destination du système ministériel Éliot. L'idée
est que certains comptes existent uniquement dans l'ENT (un compte invité créé localement, un
administrateur local...) et doivent malgré tout remonter côté national pour que la vue d'ensemble reste
cohérente.

### Plannings (EDT/Pronote, UDT)

Ce sont des formats à part, pour importer les emplois du temps. Ils stockent les cours dans une autre
base de données (MongoDB), parce que le volume et la nature des données (des milliers de créneaux qui
changent chaque semaine) ne se prêtent pas au même modèle que les comptes/structures. Il existe deux
usages différents : un import initial complet (qui crée réellement une structure et ses comptes à partir
d'un export Pronote, et qui à ce moment-là utilise bien le même moteur générique que l'AAF), et une
synchronisation incrémentale au fil de l'eau (qui, elle, a son propre mécanisme de mise à jour, séparé du
moteur générique, car il s'agit surtout de mettre à jour des cours et des rattachements de groupes plutôt
que de créer des comptes).

## Configuration

Le comportement du module (quels cron sont actifs, quels délais de suppression, si l'export automatique
est activé, quelles sources sont éligibles à la détection de doublons, etc.) est entièrement piloté par
un objet de configuration JSON passé au démarrage du verticle — il n'y a pas de fichier de configuration
séparé à éditer dans le dépôt lui-même en dehors de `mod.json` (qui ne fixe que l'adresse par défaut du
bus et le chemin des fichiers). La référence complète des clés de configuration disponibles est dans
[CLAUDE.md](CLAUDE.md#configuration--clés-utiles).

## Pour aller plus loin

- Le rapport généré par un import (qui contient les erreurs, les enregistrements ignorés et un résumé)
  est persisté en base (MongoDB) et peut aussi être envoyé par email selon la configuration — c'est le
  premier endroit à regarder pour comprendre pourquoi tel élève ou tel enseignant n'a pas été créé.
  Cherchez les entrées de type `ignored` dans le résultat, elles précisent la raison du rejet.
- Le module `entcore/tests` contient un générateur programmatique de fichiers AAF de test
  (`entcore/tests/src/test/js/it/scenarios/directory/_aaf-generator.ts`), utile pour construire des jeux
  de données réalistes (plusieurs établissements, classes, groupes, relations enseignant/classe,
  fratries...) sans avoir à écrire du XML à la main.
- Pour une référence exhaustive orientée mécanique interne (tables de configuration, index "je veux
  modifier X", pièges connus), voir [CLAUDE.md](CLAUDE.md).
