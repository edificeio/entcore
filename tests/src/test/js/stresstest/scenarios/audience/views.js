import http from 'k6/http';
import { check } from 'k6';
import chai, { describe } from 'https://jslib.k6.io/k6chaijs/4.3.4.2/index.js';
import {authenticateWeb, getHeaders} from '../../utils/user.utils.js';
import { getSchoolByName, getUsersOfSchool } from '../../utils/structure.utils.js';
import {initViewsData} from './views-init.js'

const rootUrl = __ENV.ROOT_URL;
const duration = __ENV.DURATION || '60s';
const vus = parseInt(__ENV.VUS || '1');
const nbUsers = parseInt(__ENV.NB_USERS || '10');
const schoolName = __ENV.DATA_SCHOOL_NAME || "Tests Audience"
const dataRootPath = __ENV.DATA_ROOT_PATH;

chai.config.logFailures = true;


export const options = {
  setupTimeout: '1h',
  thresholds: {
    checks: ['rate == 1.00'],
  },
  scenarios: {
    registerViews: {
     exec: 'registerViews',
     executor: "constant-vus",
     duration: duration,
     vus: vus
    },
    getViewsCounts: {
     exec: 'getViewsCounts',
     executor: "constant-vus",
     duration: duration,
     vus: vus
    }
  }
};

const teacherData = open(`${dataRootPath}/audience/enseignants.csv`, 'b')

export function setup() {
  initViewsData(schoolName, teacherData)
  const session = authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);
  const school = getSchoolByName(schoolName, session)
  const users = getUsersOfSchool(school, session)
  let ids = []
  for(let i = 0; i < Math.min(users.length, nbUsers); i++) {
    const user = users[i]
    const userSession = authenticateWeb(user.login, 'password')
    ids = ids.concat(getAllPosts(userSession))
  }
  return {users, postIds: ids};
}


export function registerViews(data) {
    const {users, postIds} = data
    const user = users[Math.floor(Math.random() * users.length)]
    const session = authenticateWeb(user.login, 'password');
    describe("[WEB] - Check can increment views", () => {
        for(let postId of postIds) {
          check(viewPost(postId, session), {
            "should get an OK response from views incrementation": r => r.status == 200
          });
        }
    });
}
export function getViewsCounts(data) {
  const {users, postIds} = data
  const user = users[Math.floor(Math.random() * users.length)]
  const session = authenticateWeb(user.login, 'password');
  describe("[WEB] - Check can view counts", () => {
    for(let postId of postIds) {
      check(viewDetails(postId, session), {
        "should get an OK response from views details": r => r.status == 200
      })
      check(viewSummary(postId, session), {
        "should get an OK response from views summary": r => r.status == 200
      })
    }
  });
}

function viewPost(postId, session) {
  return http.post(`${rootUrl}/audience/views/blog/post/${postId}`, {}, {headers: getHeaders(session)});
}

function viewSummary(postId, session) {
  return http.get(`${rootUrl}/audience/views/count/blog/post?resourceIds=${postId}`, {}, {headers: getHeaders(session)});
}

function viewDetails(postId, session) {
  return http.get(`${rootUrl}/audience/views/details/blog/post/${postId}`, {}, {headers: getHeaders(session)});
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
