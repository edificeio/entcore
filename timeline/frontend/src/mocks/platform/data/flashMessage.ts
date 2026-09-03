/**
 * Fixture for GET /timeline/flashmsg/listuser.
 * `endDate` must stay far in the future — the framework filters out expired
 * messages, so a past/near date silently makes the message disappear.
 */
export const mockFlashMessages = [
  {
    id: 'flash-1',
    title: 'Message d’information',
    contents: { fr: 'Bienvenue sur le portail.' },
    startDate: '2026-01-01T00:00:00.000',
    endDate: '2050-01-01T00:00:00.000',
    readCount: 0,
    author: 'Fake Admin',
    color: 'info',
  },
  {
    id: 'flash-2',
    title: 'Maintenance planifiée',
    contents: { fr: 'Une maintenance aura lieu ce week-end.' },
    startDate: '2026-01-01T00:00:00.000',
    endDate: '2050-01-01T00:00:00.000',
    readCount: 0,
    author: 'Fake Admin',
    color: 'warning',
  },
];
