/**
 * Fixture for GET /actualites/api/v1/infos/preview/last/6 (LastInfosContainer).
 */
export const mockLastInfos = [
  {
    id: 1,
    modifiedDate: '2026-08-20T16:36:05.398',
    headline: true,
    thread: {
      id: 101,
      icon: '/workspace/document/thread-icon-1',
      title: 'Actualités du collège',
    },
    title: 'Rentrée scolaire 2026',
    content: 'Toutes les informations pour la rentrée.',
    username: 'FAKE ADMIN',
  },
  {
    id: 2,
    modifiedDate: '2026-08-18T09:12:00.000',
    headline: false,
    thread: {
      id: 102,
      icon: '/workspace/document/thread-icon-2',
      title: 'Vie scolaire',
    },
    title: 'Sortie pédagogique',
    content: 'Une sortie est organisée le mois prochain.',
    username: 'FAKE ADMIN',
  },
];
