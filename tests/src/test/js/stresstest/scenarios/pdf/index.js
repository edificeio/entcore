import { SharedArray } from "k6/data";
import chai, { describe } from "https://jslib.k6.io/k6chaijs/4.3.4.2/index.js";
import papaparse from "https://jslib.k6.io/papaparse/5.1.1/index.js";
import {
  authenticateWeb,
  switchSession,
  Session,
} from "https://raw.githubusercontent.com/edificeio/edifice-k6-commons/develop/dist/index.js";
import { createBlog, previewFile, printBlog } from "./helper.js";
const dataRootPath = __ENV.DATA_ROOT_PATH || "./";
const nbPosts = parseInt(__ENV.NB_POST || "100");
const nbPreview = parseInt(__ENV.NB_PREVIEW || "100");

chai.config.logFailures = true;

export const options = {
  setupTimeout: "1h",
  thresholds: {
    checks: ["rate>0.9"],
  },
  scenarios: {
    shareFile: {
      executor: "ramping-vus",
      startVUs: 2,
      stages: [
        { duration: "30s", target: 10 },
        { duration: "60s", target: 40 },
        { duration: "60s", target: 100 },
      ],
      gracefulRampDown: "60s",
    },
    /*
    shareFile: {
      executor: "shared-iterations",
      vus: 1,
      iterations: 1,
    },
    */
  },
};

// Load CSV data using SharedArray to reduce memory consumption
const users = new SharedArray("users", function () {
  // Parse the CSV file with header option enabled
  return papaparse.parse(open(`${dataRootPath}/pdf/accounts.csv`), {
    header: true,
  }).data;
});

export function setup() {
  const sessions = {};
  describe("[Pdf][Auth] Connect users", () => {
    console.log(`Authenticating ${users.length} users`);
    for (let user of users) {
      const session = authenticateWeb(user.email, user.password);
      sessions[user.email] = session;
    }
    console.log(`Users authenticated`);
  });
  return { sessions };
}
export default ({ sessions }) => {
  const blogIds = []
  describe("[Pdf][Blog] Create Blog And Post", () => {
    const randomIndex = Math.floor(Math.random() * users.length);
    const user = users[randomIndex];
    const session = Session.from(sessions[user.email]);
    switchSession(session);
    createBlog(session, user, nbPosts);
  });
  describe("[Pdf][Blog] Create Blog And Post", () => {
    const randomIndex = Math.floor(Math.random() * users.length);
    const user = users[randomIndex];
    const session = Session.from(sessions[user.email]);
    switchSession(session);
    blogIds.push(createBlog(session, user, nbPosts));
  });
  describe("[Pdf][Print] Print Blog", () => {
    const randomIndex = Math.floor(Math.random() * users.length);
    const user = users[randomIndex];
    const session = Session.from(sessions[user.email]);
    switchSession(session);
    for(const id of blogIds){
        printBlog(id, session);
    }
  });
  describe("[Pdf][Print] Preview ODT File", () => {
    const randomIndex = Math.floor(Math.random() * users.length);
    const user = users[randomIndex];
    const session = Session.from(sessions[user.email]);
    switchSession(session);
    for(let i = 0 ; i < nbPreview; i++){
      previewFile(session);
    }
  });
};
