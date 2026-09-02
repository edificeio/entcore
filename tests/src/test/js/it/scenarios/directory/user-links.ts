import { check, group } from "k6";
import http from "k6/http";
import {
  authenticateWeb,
  getHeaders,
  getRandomUser,
  getUsersOfSchool,
  initStructure,
  Structure,
  UserInfo,
} from "../../../node_modules/edifice-k6-commons/dist/index.js";

const rootUrl = __ENV.ROOT_URL;
const schoolName = __ENV.DATA_SCHOOL_NAME || "IT UserLinks";
const maxDuration = __ENV.MAX_DURATION || "5m";
const gracefulStop = __ENV.GRACEFUL_STOP || "2s";

// Limite appliquée par UserLinkServiceImpl.createLink : l'insertion est ignorée
// au-delà de 10 liens pour un même utilisateur.
const MAX_LINKS = 10;
// Clé d'erreur renvoyée quand la limite est atteinte.
const LIMIT_ERROR = "directory.user.link.limit.reached";
// Longueur maximale du nom, appliquée par UserLinkValidator.
const NAME_MAX_LENGTH = 80;
// Clé d'erreur renvoyée quand le nom dépasse cette longueur.
const NAME_ERROR = "directory.user.link.name.too.long";
// Clé d'erreur renvoyée quand l'url est absente ou vide.
const URL_ERROR = "directory.user.link.url.required";

export const options = {
  setupTimeout: "1h",
  thresholds: {
    checks: ["rate == 1.00"],
  },
  scenarios: {
    testUserLinks: {
      executor: "per-vu-iterations",
      exec: "testUserLinks",
      vus: 1,
      maxDuration: maxDuration,
      gracefulStop,
    },
  },
};

type InitData = {
  users: UserInfo[];
};

type Link = {
  id: string;
  name: string;
  url: string;
};

export function setup(): InitData {
  let structure: Structure;
  let users: UserInfo[];

  group("[UserLinks] Initialize data", () => {
    authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);
    structure = initStructure(`${schoolName}`, "tiny");
    users = getUsersOfSchool(structure);
  });

  return { users };
}

function createLink(name: string, url?: string) {
  return http.post(
    `${rootUrl}/directory/user-links`,
    JSON.stringify({ name, url }),
    { headers: getHeaders("application/json") }
  );
}

function getLinksResponse() {
  return http.get(`${rootUrl}/directory/user-links`, { headers: getHeaders() });
}

function getLinks(): Link[] {
  return JSON.parse(<string>getLinksResponse().body);
}

function deleteLink(id: string) {
  return http.del(`${rootUrl}/directory/user-links/${id}`, null, {
    headers: getHeaders(),
  });
}

// Le scénario doit être rejouable sur une structure déjà initialisée : on repart
// systématiquement d'une liste vide pour l'utilisateur connecté.
function resetLinks() {
  getLinks().forEach((link) => deleteLink(link.id));
}

export function testUserLinks(data: InitData) {
  const owner = getRandomUser(data.users);
  const other = getRandomUser(data.users, [owner]);

  group("[UserLinks] GET /user-links - empty list", () => {
    authenticateWeb(owner.login);
    resetLinks();
    const res = getLinksResponse();
    const body = JSON.parse(<string>res.body);
    check(res, {
      "list links returns 200": (r) => r.status === 200,
      "list links is an array": () => Array.isArray(body),
      "list links is empty": () => body.length === 0,
    });
  });

  group("[UserLinks] POST /user-links - create a link", () => {
    authenticateWeb(owner.login);
    const name = `k6-link-${Date.now()}`;
    const url = "https://one.edifice.io";
    const res = createLink(name, url);
    check(res, {
      "create link returns 200": (r) => r.status === 200,
    });

    const links = getLinks();
    check(links, {
      "created link is listed": (l) => l.length === 1,
      "created link has an id": (l) => !!l[0].id,
      "created link has the right name": (l) => l[0].name === name,
      "created link has the right url": (l) => l[0].url === url,
    });
  });

  group("[UserLinks] DELETE /user-links/:id - delete a link", () => {
    authenticateWeb(owner.login);
    const [link] = getLinks();
    const res = deleteLink(link.id);
    check(res, {
      "delete link returns 200": (r) => r.status === 200,
    });
    check(getLinks(), {
      "deleted link is no longer listed": (l) =>
        !l.some((remaining) => remaining.id === link.id),
    });
  });

  group("[UserLinks] DELETE /user-links/:id - unknown link", () => {
    authenticateWeb(owner.login);
    const res = deleteLink("00000000-0000-0000-0000-000000000000");
    check(res, {
      "delete unknown link returns 400": (r) => r.status === 400,
    });
  });

  group("[UserLinks] DELETE /user-links/:id - malformed id", () => {
    authenticateWeb(owner.login);
    const res = deleteLink("not-a-uuid");
    check(res, {
      "delete malformed link id returns 400": (r) => r.status === 400,
    });
  });

  group("[UserLinks] POST /user-links - url is required", () => {
    authenticateWeb(owner.login);
    resetLinks();

    const cases: [string, string | undefined][] = [
      ["missing", undefined],
      ["blank", "   "],
    ];
    for (const [label, url] of cases) {
      const res = createLink("k6-link-without-url", url);
      const body = JSON.parse(<string>res.body);
      check(res, {
        [`create link with ${label} url returns 400`]: (r) => r.status === 400,
        [`create link with ${label} url returns the error key`]: () =>
          body.error === URL_ERROR,
      });
    }
    check(getLinks(), {
      "link without url is not created": (l) => l.length === 0,
    });
  });

  group(`[UserLinks] POST /user-links - name of ${NAME_MAX_LENGTH} chars max`, () => {
    authenticateWeb(owner.login);
    resetLinks();

    const tooLong = createLink(
      "n".repeat(NAME_MAX_LENGTH + 1),
      "https://one.edifice.io"
    );
    const body = JSON.parse(<string>tooLong.body);
    check(tooLong, {
      "create link with too long a name returns 400": (r) => r.status === 400,
      "create link with too long a name returns the error key": () =>
        body.error === NAME_ERROR,
    });
    check(getLinks(), {
      "link with too long a name is not created": (l) => l.length === 0,
    });

    const atLimit = createLink(
      "n".repeat(NAME_MAX_LENGTH),
      "https://one.edifice.io"
    );
    check(atLimit, {
      [`create link with a name of exactly ${NAME_MAX_LENGTH} chars returns 200`]:
        (r) => r.status === 200,
    });
  });

  group(`[UserLinks] POST /user-links - limit of ${MAX_LINKS} links`, () => {
    authenticateWeb(owner.login);
    resetLinks();
    for (let i = 0; i < MAX_LINKS; i++) {
      const res = createLink(`k6-link-${i}`, `https://one.edifice.io/${i}`);
      check(res, {
        [`create link ${i + 1} of ${MAX_LINKS} returns 200`]: (r) =>
          r.status === 200,
      });
    }
    check(getLinks(), {
      [`user has ${MAX_LINKS} links`]: (l) => l.length === MAX_LINKS,
    });

    const res = createLink("k6-link-too-many", "https://one.edifice.io/toomany");
    const body = JSON.parse(<string>res.body);
    check(res, {
      "create link beyond the limit returns 409": (r) => r.status === 409,
      "create link beyond the limit returns the error key": () =>
        body.error === LIMIT_ERROR,
    });
    check(getLinks(), {
      [`user still has ${MAX_LINKS} links`]: (l) => l.length === MAX_LINKS,
    });
  });

  group("[UserLinks] Links are private to their owner", () => {
    authenticateWeb(owner.login);
    const ownerLinks = getLinks();

    authenticateWeb(other.login);
    resetLinks();
    check(getLinks(), {
      "another user does not see the owner links": (l) => l.length === 0,
    });

    const res = deleteLink(ownerLinks[0].id);
    check(res, {
      "another user cannot delete the owner links": (r) => r.status === 400,
    });

    authenticateWeb(owner.login);
    check(getLinks(), {
      "owner links are untouched": (l) => l.length === ownerLinks.length,
    });
  });
}
