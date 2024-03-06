import http from 'k6/http';
import { check } from 'k6';
import chai, { describe } from 'https://jslib.k6.io/k6chaijs/4.3.4.2/index.js';
import {
  authenticateWeb, 
  getHeaders,
  getUsersOfSchool,
  createStructure,
  createAndSetRole,
  linkRoleToUsers,
  getTeacherRole,
  activateUsers,
  assertOk,
  Session,
  switchSession
} from "https://raw.githubusercontent.com/juniorode/edifice-k6-commons/develop/dist/index.js";

const rootUrl = __ENV.ROOT_URL;
const duration = __ENV.DURATION || '60s';
const vus = parseInt(__ENV.VUS || '1');
const nbUsers = parseInt(__ENV.NB_USERS || '10');
const schoolName = __ENV.DATA_SCHOOL_NAME || "Tests Audience"
const dataRootPath = __ENV.DATA_ROOT_PATH;
const NB_POSTS = parseInt(__ENV.AUDIENCE_NB_POSTS_PER_USER || 2);
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
  let session = authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);
  let users;
  let ids = [];
  let sessions = {}
  describe("[Audience-Init] Initialize views and reaction data", () => {
    const school = createStructure(schoolName, teacherData, session);
    const role = createAndSetRole('Blog', session);
    const groups = [
      `Teachers from group ${school.name}.`,
      `Enseignants du groupe ${school.name}.`,
      `Students from group ${school.name}.`,
      `Élèves du groupe ${school.name}.`,
      `Relatives from group ${school.name}.`,
      `Parents du groupe ${school.name}.`
    ]
    linkRoleToUsers(school, role, groups, session);
    activateUsers(school, session);
    session = authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);
    users = getUsersOfSchool(school, session).filter(u => u.type === 'Teacher')
    createPosts(school, users, session);
    for(let i = 0; i < Math.min(users.length, nbUsers); i++) {
      const user = users[i]
      const userSession = authenticateWeb(user.login, 'password')
      ids = ids.concat(getAllPosts(userSession))
      sessions[user.login] = userSession
    }
  });
  return {users, postIds: ids, sessions};
}


export function registerViews(data) {
    const {users, postIds, sessions} = data
    const user = users[Math.floor(Math.random() * users.length)]
  const session = Session.from(sessions[user.login]);
    switchSession(session)
    describe("[WEB] - Check can increment views", () => {
      const postId = postIds[Math.floor(Math.random() * postIds.length)]
      const res = viewPost(postId, session);
      const ok = check(res, {
        "should get an OK response from views incrementation": r => r.status == 200
      });
      if(!ok) {
        console.log(res)
      }
    });
}
export function getViewsCounts(data) {
  const {users, postIds, sessions} = data
  const user = users[Math.floor(Math.random() * users.length)]
  const session = Session.from(sessions[user.login]);
  switchSession(session)
  describe("[WEB] - Check can view counts", () => {
    const postId = postIds[Math.floor(Math.random() * postIds.length)]
    check(viewDetails(postId, session), {
      "should get an OK response from views details": r => r.status == 200
    })
    check(viewSummary(postId, session), {
      "should get an OK response from views summary": r => r.status == 200
    })
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
  assertOk(response, 'get all posts')
  const blogIds = JSON.parse(response.body).map(elt => elt._id);
  const postIds = [];
  blogIds.forEach(blogId => {
    const fetchedPosts = http.get(`${rootUrl}/blog/post/list/all/${blogId}`, {headers: getHeaders(session)});
    JSON.parse(fetchedPosts.body).map(post => postIds.push(post._id))
  });
  return postIds;
}



export function createPosts(structure, users, session) {
  describe("[Audience-Init] Create posts for users of structure", () => {
    const groupIds = [getTeacherRole(structure, session).id];
    for(let user of users) {
      if(!user.code) {
        const userSession = authenticateWeb(user.login, 'password')
        const headers = getHeaders(userSession)
        let res = http.get(`${rootUrl}/blog/list/all`, {headers})
        const blogs = JSON.parse(res.body)
        let blogId
        const blogName = `Blog - Stress Test Audience - ${user.login}`
        const audienceBlog = blogs.filter(blog => blog.title === blogName)[0]
        if(audienceBlog) {
          blogId = audienceBlog['_id']
          console.log('Blog already created')
        } else {
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
          blogId = JSON.parse(res.body)['_id']
        }
        const groups = {}
        groupIds.forEach(groupId => groups[groupId] = ["org-entcore-blog-controllers-PostController|list","org-entcore-blog-controllers-BlogController|get","org-entcore-blog-controllers-PostController|comments","org-entcore-blog-controllers-PostController|get","org-entcore-blog-controllers-PostController|updateComment","org-entcore-blog-controllers-PostController|deleteComment","org-entcore-blog-controllers-PostController|comment"])
        const sharePayload = {
          bookmarks: {},
          groups: groups
        };
        res = http.put(`${rootUrl}/blog/share/resource/${blogId}`, JSON.stringify(sharePayload), {headers})
        assertOk(res, 'share blog')
        const posts = JSON.parse(http.get(`${rootUrl}/blog/post/list/all/${blogId}`, {headers}).body)
        if(posts.length >= NB_POSTS) {
          console.log('Posts already created')
        } else {
          const nbPosts = NB_POSTS - posts.length
          for(let i = 0; i < nbPosts; i++) {
            const postPayload = JSON.stringify({
              title: `Post ${String(i).padStart(5, '0')} de ${user.login}`,
              content: `Le contenu du super post ${String(i + posts.length).padStart(5, '0')} du blog de ${user.login}`,
            })
            res = http.post(`${rootUrl}/blog/post/${blogId}`, postPayload, {headers})
            assertOk(res, 'create post')
            const postId = JSON.parse(res.body)['_id']
            res = http.put(`${rootUrl}/blog/post/publish/${blogId}/${postId}`, {}, {headers})
            assertOk(res, 'publish post')
          }
        }
      }
    }
  })
}
