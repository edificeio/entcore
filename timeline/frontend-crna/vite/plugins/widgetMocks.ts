import type { ServerResponse } from 'node:http';
import type { Connect, Plugin } from 'vite';

/**
 * Dev only: answers the homepage widgets' endpoints with demo data so every
 * widget renders a filled state, whatever the platform/account returns.
 * Enabled with VITE_MOCK_WIDGETS=true; every other request goes to the proxy.
 */

const DAY_MS = 24 * 60 * 60 * 1000;
const isoDate = (date: Date) => date.toISOString().slice(0, 10);
const inDays = (days: number, hour = 0) => {
  const date = new Date(Date.now() + days * DAY_MS);
  date.setHours(hour, 0, 0, 0);
  return date;
};

/** Small coloured tile with initials, used as illustration placeholder. */
const tile = (text: string, background: string, color = '#fff') =>
  `data:image/svg+xml;utf8,${encodeURIComponent(
    `<svg xmlns="http://www.w3.org/2000/svg" width="96" height="96"><rect width="96" height="96" rx="16" fill="${background}"/><text x="48" y="58" font-family="Arial" font-size="30" font-weight="bold" fill="${color}" text-anchor="middle">${text}</text></svg>`,
  )}`;

const mediacentreFavorites = () => ({
  event: 'get_favorites',
  state: 'ok',
  status: 'ok',
  data: [
    [
      'lelivrescolaire',
      'Lelivrescolaire.fr',
      'Manuels numériques',
      'LLS',
      '#2a9d8f',
    ],
    [
      'universalis',
      'Universalis Éducation',
      'Encyclopédie de référence',
      'EU',
      '#1d3557',
    ],
    ['lumni', 'Lumni', 'Vidéos éducatives', 'Lu', '#e76f51'],
    ['onisep', 'Onisep', 'Orientation et métiers', 'On', '#6a4c93'],
  ].map(([id, title, description, initials, color]) => ({
    _id: id,
    id,
    title,
    plain_text: description,
    link: `https://www.${id}.fr`,
    image: tile(initials, color),
    editors: [title],
    authors: [],
    disciplines: [],
    levels: [],
    document_types: ['Site web'],
    source: 'fr.openent.mediacentre.source.GAR',
    favorite: true,
    date: Date.now(),
    structure_name: 'Collège Charlemagne',
    structure_uai: '0000000A',
  })),
});

const mediacentrePins = () =>
  [
    [
      'pix',
      'Pix',
      'Évaluer et certifier ses compétences numériques',
      'Px',
      '#264653',
    ],
    [
      'edutheque',
      'Éduthèque',
      'Ressources des grands établissements culturels',
      'Ed',
      '#9c6644',
    ],
  ].map(([id, title, description, initials, color]) => ({
    _id: id,
    id,
    title,
    plain_text: description,
    pinned_title: title,
    pinned_description: description,
    is_pinned: true,
    link: `https://www.${id}.fr`,
    image: tile(initials, color),
    editors: [title],
    authors: [],
    disciplines: [],
    levels: [],
    document_types: ['Site web'],
    source: 'fr.openent.mediacentre.source.Signet',
    date: Date.now(),
    structure_name: 'Collège Charlemagne',
    structure_uai: '0000000A',
  }));

/** Published signets; the widget keeps the ones typed "Orientation". */
const mediacentreSignets = () => ({
  event: 'get_signets',
  state: 'ok',
  status: 'ok',
  data: {
    signets: {
      source: 'fr.openent.mediacentre.source.Signet',
      resources: [
        [
          'onisep',
          'Onisep',
          ['Métiers, formations, orientation'],
          'On',
          '#6a4c93',
        ],
        [
          'parcoursup',
          'Parcoursup',
          ["Plateforme d'admission post-bac"],
          'Ps',
          '#e63946',
        ],
        [
          'ideo',
          'Horizons 21',
          ['Choisir ses spécialités au lycée'],
          'H21',
          '#457b9d',
        ],
      ].map(([id, title, plainText, initials, color]) => ({
        id,
        title,
        image: tile(initials as string, color as string),
        link: `https://www.${id}.fr`,
        plain_text: plainText,
        document_types: ['Orientation'],
      })),
    },
  },
});

const dish = (
  type: string,
  nom: string,
  flags: Record<string, unknown> = {},
) => ({
  type,
  nom,
  ...flags,
});

const cantineMenu = () => ({
  menu: [
    dish('entree', 'Carottes râpées au citron', {
      bio: true,
      vegetarien: true,
    }),
    dish('plat', 'Blanquette de veau', { faitmaison: true, allerg_lait: 1 }),
    dish('accompagnement', 'Riz basmati', { vegetarien: true }),
    dish('laitage', 'Yaourt nature', { local: true, allerg_lait: 1 }),
    dish('dessert', 'Compote pomme-poire', { bio: true, vegetarien: true }),
  ],
  dinnerAvailable: true,
  dinnerMenu: [
    dish('entree', 'Velouté de potiron', {
      vegetarien: true,
      faitmaison: true,
    }),
    dish('plat', 'Lasagnes aux légumes', {
      vegetarien: true,
      allerg_gluten: 1,
    }),
    dish('dessert', 'Fruit de saison', { local: true }),
  ],
});

const edumalinFeed = () => ({
  data: [
    {
      title: 'Webinaire : bien préparer le brevet',
      description:
        'Méthodes de révision et gestion du stress avant les épreuves.',
      type: { name: 'Webinaire' },
      dateTime: inDays(3, 17).toISOString().slice(0, 19),
      duration: 45,
      buttonLink: 'https://www.edumalin.fr',
      buttonTitle: "S'inscrire",
    },
    {
      title: 'Nouveau parcours : apprendre à apprendre',
      description: 'Cinq séances courtes pour organiser son travail personnel.',
      type: { name: 'Actualité' },
    },
    {
      title: 'Podcast : la mémoire en 10 minutes',
      description:
        'Comprendre comment on retient et comment réviser efficacement.',
      type: { name: 'Podcast' },
      duration: 10,
    },
  ],
});

const briefMeTitles: Record<string, string[]> = {
  briefme: [
    "Ce qu'il faut retenir de l'actualité de la semaine",
    'Élections : comment fonctionne le scrutin ?',
    'Climat : les engagements des pays en 5 chiffres',
    "L'intelligence artificielle à l'école, ça change quoi ?",
    'Sport : les résultats du week-end',
  ],
  briefeco: [
    "Comprendre l'inflation en 3 minutes",
    'Pourquoi le prix de l’énergie varie autant',
    'Les jeunes et le premier emploi',
  ],
  briefscience: [
    'Une nouvelle exoplanète découverte',
    'Comment les abeilles communiquent',
    'Le cerveau pendant le sommeil',
  ],
};

const briefMeFeed = (category: string) => ({
  results: (briefMeTitles[category] ?? briefMeTitles.briefme).map(
    (title, index) => ({
      title,
      published_at: isoDate(inDays(-index)),
      url: `https://www.brief.me/${category}/${index + 1}`,
    }),
  ),
});

/** Varies the daily mock per requested date so "today" and "yesterday" differ. */
const screenTimeDaily = (date: string | null) => {
  const shift = date ? Number(date.slice(-2)) % 4 : 0;
  return {
    durations: ['08', '09', '10', '11', '13', '14', '15', '16', '17', '18']
      .map((hour, index) => ({
        hour,
        durationMinutes: [25, 40, 35, 20, 30, 45, 50, 15, 30, 20][
          (index + shift) % 10
        ],
        schoolUsePercentage: [90, 85, 80, 70, 75, 90, 85, 40, 30, 20][index],
      }))
      .slice(0, 10 - shift),
  };
};

const screenTimeWeekly = (startDate: string | null, endDate: string | null) => {
  const start = startDate ? new Date(startDate) : inDays(-6);
  const end = endDate ? new Date(endDate) : new Date();
  const days: {
    date: string;
    durationMinutes: number;
    schoolUsePercentage: number;
  }[] = [];
  for (
    let day = new Date(start), i = 0;
    day <= end;
    day = new Date(day.getTime() + DAY_MS), i++
  ) {
    days.push({
      date: isoDate(day),
      durationMinutes: [180, 210, 95, 240, 200, 60, 45][i % 7],
      schoolUsePercentage: [75, 80, 60, 85, 70, 20, 10][i % 7],
    });
  }
  return { dailySummaries: days };
};

const RSS_FEEDS = [
  {
    title: 'Le Monde - Éducation',
    link: 'https://www.lemonde.fr/education/rss_full.xml',
    show: 3,
  },
  {
    title: 'Futura Sciences',
    link: 'https://www.futura-sciences.com/rss/actualites.xml',
    show: 3,
  },
];

const rssChannels = () => [
  {
    _id: 'mock-rss-channel',
    feeds: RSS_FEEDS,
    owner: { userId: 'mock', displayName: 'Mock' },
    created: { $date: Date.now() },
    modified: { $date: Date.now() },
  },
];

const rssItems = (url: string | null) => {
  const feed = RSS_FEEDS.find((f) => f.link === url) ?? RSS_FEEDS[0];
  return {
    title: feed.title,
    link: feed.link,
    status: 200,
    Items: [1, 2, 3, 4].map((n) => ({
      title: `${feed.title} : article à la une n°${n}`,
      link: `${feed.link}#${n}`,
      description:
        'Résumé de l’article fourni par le flux RSS de démonstration.',
      pubDate: inDays(-n + 1, 8).toUTCString(),
    })),
  };
};

type Route = {
  method: string;
  match: RegExp;
  respond: (url: URL) => unknown;
};

const ROUTES: Route[] = [
  {
    method: 'GET',
    match: /^\/mediacentre\/favorites$/,
    respond: mediacentreFavorites,
  },
  {
    method: 'GET',
    match: /^\/mediacentre\/structures\/[^/]+\/pins$/,
    respond: mediacentrePins,
  },
  {
    method: 'GET',
    match: /^\/mediacentre\/signets$/,
    respond: mediacentreSignets,
  },
  {
    method: 'GET',
    match: /^\/appregistry\/[^/]+\/cantine\/menu$/,
    respond: cantineMenu,
  },
  {
    method: 'GET',
    match: /^\/appregistry\/edumalin\/widget$/,
    respond: edumalinFeed,
  },
  {
    method: 'GET',
    match:
      /^\/appregistry\/widget\/cache\/external\/(briefme|briefeco|briefscience)$/,
    respond: (url) => briefMeFeed(url.pathname.split('/').pop() ?? 'briefme'),
  },
  {
    method: 'GET',
    match: /^\/appregistry\/screen-time\/[^/]+\/daily$/,
    respond: (url) => screenTimeDaily(url.searchParams.get('date')),
  },
  {
    method: 'GET',
    match: /^\/appregistry\/screen-time\/[^/]+\/weekly$/,
    respond: (url) =>
      screenTimeWeekly(
        url.searchParams.get('startDate'),
        url.searchParams.get('endDate'),
      ),
  },
  { method: 'GET', match: /^\/rss\/channels$/, respond: rssChannels },
  {
    method: 'GET',
    match: /^\/rss\/feed\/items$/,
    respond: (url) => rssItems(url.searchParams.get('url')),
  },
  {
    method: 'POST',
    match: /^\/rss\/channel$/,
    respond: () => ({ _id: 'mock-rss-channel' }),
  },
  { method: 'PUT', match: /^\/rss\/channel\/[^/]+$/, respond: () => ({}) },
];

const sendJson = (res: ServerResponse, body: unknown) => {
  res.statusCode = 200;
  res.setHeader('Content-Type', 'application/json; charset=utf-8');
  res.setHeader('Cache-Control', 'no-store');
  res.end(JSON.stringify(body));
};

export function widgetMocksPlugin({ enabled }: { enabled: boolean }): Plugin {
  return {
    name: 'widget-mocks',
    apply: 'serve',
    configureServer(server) {
      if (!enabled) return;
      server.config.logger.info(
        '[widget-mocks] homepage widget endpoints are mocked',
      );

      const middleware: Connect.NextHandleFunction = (req, res, next) => {
        const url = new URL(req.url ?? '/', 'http://localhost');
        const route = ROUTES.find(
          (r) => r.method === req.method && r.match.test(url.pathname),
        );
        if (!route) return next();
        sendJson(res, route.respond(url));
      };
      server.middlewares.use(middleware);
    },
  };
}
