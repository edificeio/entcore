import { check } from "k6";
import { SharedArray } from 'k6/data';
import {chai, describe } from "https://jslib.k6.io/k6chaijs/4.3.4.0/index.js";
import papaparse from 'https://jslib.k6.io/papaparse/5.1.1/index.js';
import {
  authenticateWeb,
  uploadFile,
  switchSession,
  Session,
  getHeaders} from "https://raw.githubusercontent.com/juniorode/edifice-k6-commons/develop/dist/index.js";
import http from "k6/http";

const nbUploads =  parseInt(__ENV.NB_UPLOADS || "20");
const nbRoundsGetThumbnails =  parseInt(__ENV.NB_ROUND_GET_THUMBNAILS || "2");
const maxDuration = __ENV.MAX_DURATION || "1m";
const dataRootPath = __ENV.DATA_ROOT_PATH;
const gracefulStop = parseInt(__ENV.GRACEFUL_STOP || "2s");
const rootUrl = __ENV.ROOT_URL;

chai.config.logFailures = true;

export const options = {
  setupTimeout: "1h",
  /*thresholds: {
    checks: ["rate == 1.00"],
  },*/
  scenarios: {
    shareFile: {
      executor: "ramping-vus",
      startVUs: 2,
      stages: [
        {duration: '60s', target: 10},
        {duration: '60s', target: 20},
        {duration: '60s', target: 100},
      ],
      gracefulRampDown: '0s',
    },
  },
};

// Load CSV data using SharedArray to reduce memory consumption
const users = new SharedArray('users', function () {
  // Parse the CSV file with header option enabled
  return papaparse.parse(open(`${dataRootPath}/thumbnails/accounts.csv`), { header: true }).data;
});
const fileToUpload = open(`${dataRootPath}/thumbnails/image.jpg`, "b");


export function setup() {
  const sessions = {}
  describe("[Thumbnail] Connect users", () => {
    console.log(`Authenticating ${users.length} users`)
    for(let user of users) {
      const session = authenticateWeb(user.email, user.password);
      sessions[user.email] = session;
    }
    console.log(`Users authenticated`)
  });
  return { sessions };
}
export default ({sessions}) => {
  describe("[Thumbnail]", () => {
    const randomIndex = Math.floor(Math.random() * users.length);
    const user = users[randomIndex];
    const session = Session.from(sessions[user.email])
    switchSession(session);
    const uploadedFiles = []
    console.log("Uploading files")
    for(let i = 0; i < nbUploads; i++) {
      uploadedFiles.push(uploadFile(fileToUpload, session));
    }
    console.log("Getting thumbnails")
    for(let i = 0; i < nbRoundsGetThumbnails; i++) {
      for(let uploadedFile of uploadedFiles) {
        getThumbnail(uploadedFile, session)
      }
    }
  })
};


function getThumbnail(file, session) {
  const url = `${rootUrl}/workspace/document/${file._id}?thumbnail=150x150`;
  const headers = getHeaders(session);
  headers['Referer'] = `${rootUrl}/blog/id/93aaa579-7823-4d73-bb51-8f9a85e3227f`
  const res = http.get(url, {headers});
  check(res, {
    "thumbnail is ok": (r) => r.status === 200,
    "size is small": (r) => {
      console.log(r.headers['Content-Length'])
      return r.headers['Content-Length'] < 5569000
    }
  })
}