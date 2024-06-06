import { check, sleep } from "k6";
import { SharedArray } from 'k6/data';
import chai, { describe } from "https://jslib.k6.io/k6chaijs/4.3.4.2/index.js";
import papaparse from 'https://jslib.k6.io/papaparse/5.1.1/index.js';
import {
  authenticateWeb,
  assertOk,
  switchSession,
  Session,
  getHeaders} from "https://raw.githubusercontent.com/juniorode/edifice-k6-commons/develop/dist/index.js";
import http from "k6/http";

const dataRootPath = __ENV.DATA_ROOT_PATH;
const rootUrl = __ENV.ROOT_URL;
const nbPosts = parseInt(__ENV.NB_POST || "100");

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
        {duration: '30s', target: 10},
        {duration: '60s', target: 40},
        {duration: '60s', target: 100},
      ],
      gracefulRampDown: '60s',
    },
  },
};

// Load CSV data using SharedArray to reduce memory consumption
const users = new SharedArray('users', function () {
  // Parse the CSV file with header option enabled
  return papaparse.parse(open(`${dataRootPath}/audience/accounts.csv`), { header: true }).data;
});


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
  describe("[Charge] Create Blog And Post", () => {
    const randomIndex = Math.floor(Math.random() * users.length);
    const user = users[randomIndex];
    const session = Session.from(sessions[user.email])
    switchSession(session);
    
    const headers = getHeaders(session)
    const blogName = `Blog - Stress Test - ${user.email}`
    headers['content-type'] = 'application/json'
    const payload = JSON.stringify({
        title: blogName,
        description: `Le blog de ${user.login}`,
        thumbnail: '/blog/public/img/blog.png',
        'comment-type': 'NONE',
        'publish-type': 'RESTRAINT'
    })
    let res = http.post(`${rootUrl}/blog`, payload, {headers})
    assertOk(res, 'create blog')
    const blogId = JSON.parse(res.body)['_id']
    for(let i = 0; i < nbPosts; i++) {
        const postPayload = JSON.stringify({
            title: `Post ${String(i).padStart(5, '0')} de ${user.login}`,
            content: `Le contenu du super post ${String(i).padStart(5, '0')} du blog de ${user.login}`,
        })
        res = http.post(`${rootUrl}/blog/post/${blogId}`, postPayload, {headers})
        //sleep(0.5)
        assertOk(res, 'create post')
        const postId = JSON.parse(res.body)['_id']
        if(postId) {
            res = http.get(`${rootUrl}/blog/post/list/all/${blogId}?postId=${postId}`, {headers})
            assertOk(res, 'get post')
            check(res, {
                "post was not found right after its creation": r => {
                    const posts = JSON.parse(r.body)
                    return posts.length && posts.length > 0
                }
            })
        }
    }
})
};
