/**
 * Fixtures for GET /timeline/lastNotifications and GET /timeline/types.
 * `mockNotificationTypes` must contain every `type` present in
 * `mockNotifications`, otherwise the notification type filter returns an
 * empty list.
 */
export const mockNotifications = [
  {
    _id: 'notif-support-1',
    type: 'SUPPORT',
    eventType: 'CREATE_TICKET',
    date: { $date: Date.parse('2026-08-20T09:00:00.000Z') },
    message:
      '<a href="/directory/annuaire#a1b2c3d4">Fake User</a> a créé un ticket de support.',
    params: {
      username: 'Fake User',
      ticketId: 42,
      ticketUri: '/support#/ticket/42',
    },
  },
  {
    _id: 'notif-blog-1',
    type: 'BLOG',
    eventType: 'SHARE',
    date: { $date: Date.parse('2026-08-21T10:30:00.000Z') },
    message:
      '<a href="/directory/annuaire#a1b2c3d4">Fake User</a> vous a donné accès au blog <a href="/blog#/view/1">Blog de test</a>.',
    params: {
      username: 'Fake User',
      blogTitle: 'Blog de test',
      resourceUri: '/blog#/view/1',
    },
  },
  {
    _id: 'notif-mood-1',
    type: 'USERBOOK_MOOD',
    date: { $date: Date.parse('2026-08-22T08:15:00.000Z') },
    message:
      '<a href="/directory/annuaire#a1b2c3d4">Fake User</a> a changé son humeur.',
    params: {
      username: 'Fake User',
    },
  },
];

export const mockNotificationTypes = [
  'SUPPORT',
  'BLOG',
  'USERBOOK_MOOD',
  'MESSAGERIE',
  'CALENDAR',
  'FORMULAIRE',
  'COLLABORATIVEWALL',
  'NEWS',
];
