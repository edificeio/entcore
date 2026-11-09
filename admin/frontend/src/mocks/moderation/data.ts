import { type Case, type Reason, type Status } from '~/models';

/** Authors of the reported contents. */
const AUTHORS = [
  { id: 'u-001', firstName: 'Jean', lastName: 'QUILLE' },
  { id: 'u-002', firstName: 'Marie', lastName: 'CURIE' },
  { id: 'u-003', firstName: 'Paul', lastName: 'MARTIN' },
];

/** People who report contents. */
const REPORTERS = [
  { id: 'r-001', displayName: 'NOKA Louis' },
  { id: 'r-002', displayName: 'BERNARD Chloé' },
  { id: 'r-003', displayName: 'PETIT Hugo' },
  { id: 'r-004', displayName: 'ROUX Léa' },
];

const SOURCES = [
  { module: 'news', type: 'message', title: 'Sortie scolaire annulée' },
  { module: 'blog', type: 'post', title: 'Retour sur la kermesse' },
  { module: 'conversation', type: 'message', title: 'Re: devoirs de maths' },
  { module: 'wiki', type: 'page', title: 'Règlement intérieur' },
];

const REASONS: Reason[] = [
  'HATRED',
  'HARASSMENT',
  'VIOLENT_SCORNFUL',
  'NUDITY',
  'FAKE_NEWS',
  'OTHER',
];

const STATUSES: Status[] = [
  'OPEN',
  'OPEN',
  'PENDING',
  'QUARANTINE',
  'RESOURCE_DELETED',
  'RESOURCE_KEPT',
  'CLOSED',
];

const uuid = (prefix: string, n: number) =>
  `${prefix}-0000-4000-8000-${String(n).padStart(12, '0')}`;

const isoDate = (daysAgo: number) =>
  new Date(
    Date.UTC(2026, 10, 5, 10, 12, 36) - daysAgo * 86_400_000,
  ).toISOString();

/** Build a deterministic fake case, so the mock data is stable between reloads. */
const buildCase = (n: number): Case => {
  const author = AUTHORS[n % AUTHORS.length];
  const source = SOURCES[n % SOURCES.length];
  const reason = REASONS[n % REASONS.length];
  const status = STATUSES[n % STATUSES.length];
  const reportsCount = (n % 3) + 1;

  return {
    id: uuid('case', n),
    module: source.module,
    type: source.type,
    path: `/${source.module}/${uuid('res', n)}`,
    reason,
    createdAt: isoDate(n),
    modifiedAt: isoDate(n),
    status,
    title: `${source.title} #${n}`,
    authorId: author.id,
    authorDisplayName: `${author.lastName} ${author.firstName}`,
    authorFirstName: author.firstName,
    authorLastName: author.lastName,
    references: `${source.module.toUpperCase()}-${String(n).padStart(3, '0')}`,
    body: { type: 'doc', content: [{ type: 'text', text: 'Contenu signalé' }] },
    reports: Array.from({ length: reportsCount }, (_, i) => {
      const reporter = REPORTERS[(n + i) % REPORTERS.length];
      return {
        id: uuid(`rep${i}`, n),
        reporterId: reporter.id,
        displayName: reporter.displayName,
        // First report carries the case reason, later ones vary
        reason: i === 0 ? reason : REASONS[(n + i) % REASONS.length],
        comment: `Commentaire du signalement ${i + 1}`,
        createdAt: isoDate(n),
      };
    }),
    decisions:
      status === 'OPEN'
        ? []
        : [
            {
              id: uuid('dec', n),
              userId: 'a1b2c3d4',
              displayName: 'Super adml',
              status,
              comment: 'Décision de test',
            },
          ],
  };
};

/** Fresh copy of the mocked cases (handlers mutate it when a decision is posted). */
export const createMockCases = (count = 45): Case[] =>
  Array.from({ length: count }, (_, i) => buildCase(i + 1));
