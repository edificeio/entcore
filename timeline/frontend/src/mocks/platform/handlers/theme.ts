import { HttpResponse, http } from 'msw';

export const handlers = [
  http.get('/theme', () =>
    HttpResponse.json({
      template: '/public/template/portal.html',
      logoutCallback: '',
      skin: '/assets/themes/fake/skins/default/',
      themeName: 'fake-theme',
      skinName: 'default',
    }),
  ),

  http.get('/assets/theme-conf.js', () =>
    HttpResponse.json({
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
    }),
  ),
];
