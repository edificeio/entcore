import { HttpResponse, http } from 'msw';
import { baseUrl } from '~/services';
import {
  mockCountOfMessagesInInbox,
  mockFolderTree,
  mockFullMessage,
  mockMessagesOfInbox,
  mockSentMessage,
} from '.';

/**
 * DO NOT MODIFY
 */
const defaultHandlers = [
  http.get('/userbook/preference/apps', () => {
    return HttpResponse.json({
      preference: '{"bookmarks":[],"applications":["FakeApp"]}',
    });
  }),

  http.get('/i18n', () => {
    return HttpResponse.json({ status: 200 });
  }),

  http.get('/userbook/api/person', () => {
    return HttpResponse.json({
      status: 'ok',
      result: [
        {
          id: 'a1b2c3d4',
          login: 'fake.user',
          displayName: 'Fake User',
          type: ['Personnel'],
          visibleInfos: [],
          schools: [
            {
              exports: null,
              classes: [],
              name: 'Fake School',
              id: 'd4c3b2a1',
              UAI: null,
            },
          ],
          relatedName: null,
          relatedId: null,
          relatedType: null,
          userId: 'a1b2c3d4',
          motto: 'Always Learning',
          photo: '/userbook/avatar/a1b2c3d4',
          mood: 'happy',
          health: 'good',
          address: '123 Fake Street',
          email: 'fake.user@example.com',
          tel: '1234567890',
          mobile: '0987654321',
          birthdate: '1990-01-01',
          hobbies: ['reading', 'coding'],
        },
      ],
    });
  }),

  http.get('/theme', () => {
    return HttpResponse.json({
      template: '/public/template/portal.html',
      logoutCallback: '',
      skin: '/assets/themes/fake/skins/default/',
      themeName: 'fake-theme',
      skinName: 'default',
    });
  }),

  http.get('/locale', () => {
    return HttpResponse.json({ locale: 'fr' });
  }),

  http.get('/directory/userbook/a1b2c3d4', () => {
    return HttpResponse.json({
      mood: 'happy',
      health: 'good',
      alertSize: false,
      storage: 12345678,
      type: 'USERBOOK',
      userid: 'a1b2c3d4',
      picture: '/userbook/avatar/a1b2c3d4',
      quota: 104857600,
      motto: 'Always Learning',
      theme: 'default',
      hobbies: ['reading', 'coding'],
    });
  }),

  http.get('/userbook/preference/language', () => {
    return HttpResponse.json({
      preference: '{"default-domain":"fr"}',
    });
  }),

  http.get('/workspace/quota/user/a1b2c3d4', () => {
    return HttpResponse.json({ quota: 104857600, storage: 12345678 });
  }),

  http.get('/auth/oauth2/userinfo', () => {
    return HttpResponse.json({
      classNames: null,
      level: '',
      login: 'fake.admin',
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
      userId: 'a1b2c3d4',
      structures: ['d4c3b2a1'],
      structureNames: ['Fake School'],
      uai: [],
      hasApp: false,
      ignoreMFA: true,
      classes: [],
      authorizedActions: [
        {
          name: 'org.entcore.fake.controllers.FoldersController|add',
          displayName: 'fake.createFolder',
          type: 'SECURED_ACTION_WORKFLOW',
        },
        {
          name: 'org.entcore.fake.controllers.FoldersController|list',
          displayName: 'fake.listFolders',
          type: 'SECURED_ACTION_WORKFLOW',
        },
        {
          name: 'org.entcore.fake.controllers.FakeController|print',
          displayName: 'fake.print',
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
    });
  }),

  http.get('/userbook/preference/rgpdCookies', () => {
    return HttpResponse.json({ preference: '{"showInfoTip":true}' });
  }),

  http.get('/applications-list', () => {
    return HttpResponse.json({
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
    });
  }),

  http.get('/assets/theme-conf.js', () => {
    return HttpResponse.json({
      overriding: [
        {
          parent: 'theme-open-ent',
          child: 'fake-theme',
          skins: ['default', 'colorful'],
          help: '/help-fake',
          bootstrapVersion: 'ode-bootstrap-fake',
          edumedia: {
            uri: 'https://www.fake-edumedia.com',
            pattern: 'uai-token-hash-[[uai]]',
            ignoreSubjects: ['fake-92', 'fake-93'],
          },
        },
        {
          parent: 'panda',
          child: 'fake-panda',
          skins: [
            'circus',
            'desert',
            'neutre',
            'ocean',
            'fake-food',
            'sparkly',
            'default',
            'monthly',
          ],
          help: '/help-fake-panda',
          bootstrapVersion: 'ode-bootstrap-fake',
          edumedia: {
            uri: 'https://junior.fake-edumedia.com',
            pattern: 'uai-token-hash-[[uai]]',
          },
        },
      ],
    });
  }),
];

/**
 * MSW Handlers
 * Mock HTTP methods for your own application
 */
export const handlers = [
  ...defaultHandlers,
  // Folder service
  http.get(`${baseUrl}/api/folders`, () => {
    return HttpResponse.json(mockFolderTree, { status: 200 });
  }),
  http.get(`${baseUrl}/api/folders/:folderId/messages`, ({ params }) => {
    if (params['folderId'] != 'inbox') {
      return HttpResponse.text('Unexpected error', { status: 500 });
    } else {
      return HttpResponse.json(mockMessagesOfInbox, { status: 200 });
    }
  }),
  http.get(`${baseUrl}/count/:folderId`, ({ params }) => {
    if (params['folderId'] != 'inbox') {
      return HttpResponse.text('Unexpected error', { status: 500 });
    } else {
      return HttpResponse.json(mockCountOfMessagesInInbox, { status: 200 });
    }
  }),
  http.post(`${baseUrl}/conversation/folder`, () => {
    return HttpResponse.json({ id: 'folder_Z' }, { status: 201 });
  }),
  http.put(`${baseUrl}/conversation/folder/:folderId`, () => {
    return HttpResponse.json({}, { status: 200 });
  }),
  http.put(`${baseUrl}/conversation/folder/trash/:folderId`, () => {
    return HttpResponse.json({}, { status: 200 });
  }),
  // Message service
  http.get(`${baseUrl}/api/messages/:messageId`, () => {
    return HttpResponse.json(mockFullMessage, { status: 200 });
  }),
  http.post<
    object,
    {
      id: string[];
      unread: boolean;
    }
  >(`${baseUrl}/toggleUnread`, async ({ request }) => {
    const payload = await request.json();
    if (
      !payload ||
      !Array.isArray(payload.id) ||
      typeof payload.unread !== 'boolean'
    ) {
      return HttpResponse.text('Bad Request', { status: 400 });
    }
    return HttpResponse.text('', { status: 200 });
  }),
  http.put<
    object,
    {
      id: string[];
    }
  >(`${baseUrl}/conversation/restore`, async ({ request }) => {
    const payload = await request.json();
    if (!payload || !Array.isArray(payload.id)) {
      return HttpResponse.text('Bad Request', { status: 400 });
    }
    return HttpResponse.text('', { status: 200 });
  }),
  http.put<
    object,
    {
      id: string[];
    }
  >(`${baseUrl}/conversation/delete`, async ({ request }) => {
    const payload = await request.json();
    if (!payload || !Array.isArray(payload.id)) {
      return HttpResponse.text('Bad Request', { status: 400 });
    }
    return HttpResponse.text('', { status: 200 });
  }),
  http.post(`${baseUrl}/conversation/draft`, () => {
    return HttpResponse.json({ id: 'message_draft' }, { status: 201 });
  }),
  http.post(`${baseUrl}/conversation/draft/:draftId`, () => {
    return HttpResponse.text('', { status: 200 });
  }),
  http.post(`${baseUrl}/conversation/send`, async ({ request }) => {
    const url = new URL(request.url);
    const id = url.searchParams.get('id');
    const payload = await request.json();
    if (!id) {
      return HttpResponse.text('Bad Request', {
        status: 400,
      });
    }
    return HttpResponse.json(Object.assign({ id }, mockSentMessage, payload), {
      status: 200,
    });
  }),
];
