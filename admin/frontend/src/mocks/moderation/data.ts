import { type Case, type Reason, type Status } from '~/models';

const SENDERS = [
  { id: 'u-001', firstName: 'Jean', lastName: 'QUILLE' },
  { id: 'u-002', firstName: 'Marie', lastName: 'CURIE' },
  { id: 'u-003', firstName: 'Paul', lastName: 'MARTIN' },
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
  const sender = SENDERS[n % SENDERS.length];
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
    senderId: sender.id,
    senderDisplayName: `${sender.firstName} ${sender.lastName}`,
    senderFirstName: sender.firstName,
    senderLastName: sender.lastName,
    references: `${source.module.toUpperCase()}-${String(n).padStart(3, '0')}`,
    body: { type: 'doc', content: [{ type: 'text', text: 'Contenu signalé' }] },
    reports: Array.from({ length: reportsCount }, (_, i) => ({
      id: uuid(`rep${i}`, n),
      reporterId: uuid('usr', n + i),
      reason,
      comment: `Commentaire du signalement ${i + 1}`,
      createdAt: isoDate(n),
    })),
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
