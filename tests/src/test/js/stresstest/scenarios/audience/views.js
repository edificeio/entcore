import http from 'k6/http';
import { check } from 'k6';
import chai, { describe } from 'https://jslib.k6.io/k6chaijs/4.3.4.2/index.js';
import {authenticateWeb, getHeaders} from '../../utils/user.utils.js';
import { Session } from '../../utils/authentication.utils.js';
import { SharedArray } from 'k6/data';

const rootUrl = __ENV.ROOT_URL;
const dataRootPath = __ENV.DATA_ROOT_PATH;
const nbRounds = __ENV.DATA_NB_ROUNDS || 100;
const duration = __ENV.DURATION || '60s';
const vus = parseInt(__ENV.VUS || '1');

chai.config.logFailures = true;

export const options = {
  thresholds: {
    checks: ['rate == 1.00'],
  },
  scenarios: {
    view_files: {
     exec: 'viewPosts',
     executor: "per-vu-iterations",
     maxDuration: duration,
     vus: vus
    }
  }
};
const userData = new SharedArray('userData', function () {
  return JSON.parse(open(`${dataRootPath}/audience/users.json`))
});
let webSession;

export function setup() {
    webSession = new Session(null, null, -1);
}

export function viewPosts() {
    const user = userData[Math.floor(Math.random() * userData.length)]
    const session = authenticateWeb(user.login, user.password);
    const postIds = getAllPosts(session);
    describe("[WEB] - Check can view allowed posts", () => {
        for (let i = 0; i < nbRounds; i ++) {
          const response = viewPost(postIds[i], session);
          check(response, {
            "should get an OK response from views incrementation": r => r.status == 200
          })
        }
    });
}

function getAllPosts(session) {
  const response = http.get(`${rootUrl}/blog/list/all`, {headers: getHeaders(session)});
  check(response, {
    "should get all posts": r => r.status == 200
  });
  const blogIds = JSON.parse(response.body).map(elt => elt._id);
  const postIds = [];
  blogIds.forEach(blogId => {
    const fetchedPosts = http.get(`${rootUrl}/blog/post/list/all/${blogId}`, {headers: getHeaders(session)});
    JSON.parse(fetchedPosts.body).map(post => postIds.push(post._id))
  });
  return postIds;
}

function viewPost(postId, session) {
  return http.get(`${rootUrl}/audience/views/blog/post/${postId}`, {headers: getHeaders(session)});
}
