/**
 * Fixture for GET /auth/oauth2/userinfo — the whole session in one payload.
 * `authorizedActions` drives `odeServices.rights().sessionHasWorkflowRights()`
 * and `widgets` drives `useWidgetPreferences` (both read the session, no
 * separate HTTP call) — keep both in sync with what timeline actually needs.
 */
export const mockUserId = 'a1b2c3d4';

export const mockUserInfo = {
  classNames: null,
  level: '',
  login: 'fake.user',
  lastName: 'Admin',
  firstName: 'Fake',
  externalId: 'abcd1234-5678-90ef-ghij-klmn1234opqr',
  federated: null,
  birthDate: '1980-01-01',
  forceChangePassword: null,
  needRevalidateTerms: false,
  deletePending: false,
  username: 'fake.user',
  type: 'ADMIN',
  hasPw: true,
  functions: {
    SUPER_ADMIN: {
      code: 'SUPER_ADMIN',
      scope: null,
    },
  },
  groupsIds: ['group1-1234567890', 'group2-0987654321'],
  federatedIDP: null,
  optionEnabled: [],
  userId: mockUserId,
  structures: ['d4c3b2a1'],
  structureNames: ['Fake School'],
  uai: [],
  hasApp: false,
  ignoreMFA: true,
  classes: [],
  authorizedActions: [
    {
      name: 'org.entcore.timeline.controllers.TimelineController|report',
      displayName: 'timeline.report',
      type: 'SECURED_ACTION_WORKFLOW',
    },
    {
      name: 'org.entcore.timeline.controllers.TimelineController|delete',
      displayName: 'timeline.delete',
      type: 'SECURED_ACTION_WORKFLOW',
    },
  ],
  apps: [
    {
      name: 'FakeApp',
      address: '/fake',
      icon: 'fake-large',
      target: '',
      displayName: 'fake',
      display: true,
      prefix: '/fake',
      casType: null,
      scope: [''],
      isExternal: false,
    },
  ],
  childrenIds: [],
  children: {},
  widgets: [],
  sessionMetadata: {},
};
