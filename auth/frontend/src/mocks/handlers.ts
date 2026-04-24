import { HttpResponse, http } from 'msw';
import authI18nFr from '../../../src/main/resources/i18n/fr.json';

/**
 * DO NOT MODIFY
 */
const defaultHandlers = [
  http.get('/auth/i18n', () => {
    return HttpResponse.json(authI18nFr);
  }),

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

  http.get('/auth/configure/welcome', () => {
    return HttpResponse.json({
      enabled: true,
      de: '',
      co: '',
      es: '',
      pt: '',
      fr: '<h3 class="ng-scope" style="text-align: center;"><span style="background-color: transparent; font-family: Arial, sans-serif; font-size: 11pt; white-space-collapse: preserve; color: rgb(0, 0, 0); text-align: left;">​</span></h3><h1 style="line-height: 28px;" class="ng-scope"><div class="ng-scope" style=""><div class="ng-scope" style=""><span id="docs-internal-guid-fb8fb236-7fff-a1cb-f7bc-e31dd61c790a" class="ng-scope" style=""><span id="docs-internal-guid-03c41daf-7fff-fc74-0585-b5d88705fb05" style=""><div style="color: rgb(0, 0, 0); font-size: 14px; word-break: break-word; line-height: 1.38; margin-top: 12pt; margin-bottom: 12pt;"><span style="background-color: transparent; font-variant-numeric: normal; font-variant-east-asian: normal; font-variant-alternates: normal; font-variant-position: normal; font-variant-emoji: normal; white-space-collapse: preserve; font-size: 11pt; font-family: Arial, sans-serif; vertical-align: baseline;"><span><div>​</div><br></span></span></div></span></span></div></div></h1><h2 class="ng-scope"><div style="font-size: 14px;"></div></h2>',
      en: '',
      it: '',
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
export const handlers = [...defaultHandlers];
