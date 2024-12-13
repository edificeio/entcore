import http from 'k6/http';
import { check, sleep } from 'k6';
import chai, { describe, expect } from 'https://jslib.k6.io/k6chaijs/4.3.4.0/index.js';
import {authenticateOAuth2, authenticateWeb, getConnectedUserId, getHeaders, searchUser} from '../../utils/user.utils.js';
import {getMetricValue} from '../../utils/metrics.utils.js';
import {BASE_URL} from '../../utils/env.utils.js';
import { Session } from '../../utils/authentication.utils.js';
import { FormData } from 'https://jslib.k6.io/formdata/0.0.2/index.js';


const USER_MOBILE_LOGIN = __ENV.USER_MOBILE_LOGIN;
const USER_MOBILE_PWD = __ENV.USER_MOBILE_PWD;
const USER_WEB_LOGIN = __ENV.USER_WEB_LOGIN;
const USER_WEB_PWD = __ENV.USER_WEB_PWD;
const CLIENT_ID = __ENV.CLIENT_ID;
const CLIENT_PWD = __ENV.CLIENT_PWD;
const DURATION = __ENV.DURATION;
const now = Date.now();

const filePath = '../../../../resources/data/xss-light.svg';
const dataFile = open(filePath);
const data = new FormData();
data.append('file', http.file(dataFile, 'xss-light.svg', 'image/svg+xml'));

chai.config.logFailures = true;

export const options = {
  thresholds: {
    checks: ['rate == 1.00'],
  },
  scenarios: {
    send_files: {
      exec: 'sendFiles',
      executor: "constant-arrival-rate",
      startTime: "0s",
      duration: DURATION,
      rate: 1,
      timeUnit: '1s',
      preAllocatedVUs: 10,
      maxVUs: 100
    },
    check_files: {
     exec: 'checkFiles',
     executor: "per-vu-iterations",
     startTime: DURATION,
     maxDuration: '10s',
     vus: 1
    }
  }
};

let initialProcessOKMessages = -1;
let initialProcessKOMessages = -1;
const unitToMultiplier = { s: 1000, ms: 1, m: 60 * 1000, h: 60 * 60 * 1000};
const maxDuration = options.scenarios.check_files.maxDuration;
const unit = maxDuration.charAt(maxDuration.length - 1);
const value = parseInt(maxDuration.substring(0, maxDuration.length - 1));
const maxDurationInMs = value * unitToMultiplier[unit];

const sentMessages = [];
let webSession;
let webUserId;

export function setup() {
    webSession = new Session(null, null, -1);
}

export function checkFiles() {
    authenticateWeb(USER_WEB_LOGIN, USER_WEB_PWD);
    describe("[WEB] - Check metrics ok", () => {
        const timeout = Date.now() + maxDurationInMs;
        let checked = false;
        let nbProcessedOK;
        let nbProcessedKO;
        let nbProcessedTotal;
        let currentNbProcessOKMessages;
        let currentNbProcessKOMessages;
        let hasErrors = true;
        while(!checked && Date.now() <= timeout) {
            const session = webSession;
            currentNbProcessOKMessages = getMetricValue("message_process_ok_total", session);
            currentNbProcessKOMessages = getMetricValue("message_process_ko_total", session);
            nbProcessedOK = currentNbProcessOKMessages - initialProcessOKMessages;
            nbProcessedKO = currentNbProcessKOMessages - initialProcessKOMessages;
            nbProcessedTotal = nbProcessedOK + nbProcessedKO;
            if (nbProcessedTotal >= sentMessages.length) {
                hasErrors = !check(null, {
                    'no files generated errors': () => nbProcessedKO == 0,
                    'all files were OK': () => nbProcessedOK === sentMessages.length,
                    'all files were treated': () => nbProcessedOK === sentMessages.length,
                })
                checked = true;
            } else {
                sleep(2)
            }
        }
        if(hasErrors) {
            console.error("nbProcessedKO=", nbProcessedKO);
            console.error("nbProcessedOK=", nbProcessedOK, " / ", sentMessages.length);
        }
        check(null, {
            'all files were processed in time': () => checked
        })
    });
}

export function sendFiles (){
    describe("[Web] authentication", () => {
      if (webSession.isExpired()) {
        console.log("WEB - Authentication")
        webSession = authenticateWeb(USER_WEB_LOGIN, USER_WEB_PWD);
      } else {
        const jar = http.cookieJar();
        jar.set(BASE_URL, 'oneSessionId', webSession.token);
        for(const cookie of webSession.cookies) {
          jar.set(BASE_URL, cookie.name, cookie.value);
        }
      }
      const session = webSession;
      if(!webUserId) {
        webUserId = getConnectedUserId(session);
      }
    })
    if(initialProcessOKMessages < 0) {
        describe("[Web] get initial counters", () => {
          const session = webSession;
          const headers = getHeaders(session);
          initialProcessOKMessages = getMetricValue("message_process_ok_total");
          initialProcessKOMessages = getMetricValue("message_process_ko_total");
        })
    }
    describe("[Web] upload file", () => {
      const session = webSession;
      const headers = getHeaders(session);
      headers["content-type"] = "multipart/form-data; boundary=" + data.boundary;
      const responseDraftMessage = http.post(
        `${BASE_URL}/workspace/document?quality=1`,
         data.body(), { redirects: 0, headers });
      check(responseDraftMessage, {
        'message uploaded': r => r.status == 201,
        'file id received': r => !!r.json()["file"]
      });
      const fileId = responseDraftMessage.json()["file"]
      sentMessages.push(fileId);
    });
}

