import http from 'k6/http';
import { check } from 'k6';
import chai, { describe } from 'https://jslib.k6.io/k6chaijs/4.3.4.2/index.js';
import {authenticateWeb, getHeaders} from '../../utils/user.utils.js';
import { getSchoolByName } from '../../utils/structure.utils.js'
import { FormData } from 'https://jslib.k6.io/formdata/0.0.2/index.js';

const rootUrl = __ENV.ROOT_URL;
const NB_POSTS = parseInt(__ENV.AUDIENCE_NB_POSTS_PER_USER || 2);


export function initViewsData(schoolName, teachers) {
    const session = authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);
    describe("[Audience-Init] Initialize views and reaction data", () => {
        const structure = createStructure(schoolName, teachers, session);
        const role = createAndSetRole('Blog', session);
        linkRoleToUsers(structure, role, session);
        activateUsers(structure, session);
        createPosts(structure, session);
    });
}

export function createPosts(structure, session) {
  describe("[Audience-Init] Create posts for users of structure", () => {
    let res = http.get(`${rootUrl}/directory/structure/${structure.id}/users`, { headers: getHeaders(session) })
    check(res, {
      'fetch structure users': (r) => r.status == 200
    })
    const users = JSON.parse(res.body);
    const roles = getRolesOfStructure(structure.id, session);
    const groupIds = roles.filter(role => role.name.indexOf(`from group ${structure.name}.`) >= 0 ||
                                          role.name.indexOf(`Enseignants du groupe ${structure.name}.`))
                          .map(role => role.id);
    for(let user of users) {
      if(!user.code) {
        const userSession = authenticateWeb(user.login, 'password')
        const headers = getHeaders(userSession)
        res = http.get(`${rootUrl}/blog/list/all`, {headers})
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
          check(res, {
            'create blog': (r) => r.status === 200
          })
          blogId = JSON.parse(res.body)['_id']
        }
        const posts = JSON.parse(http.get(`${rootUrl}/blog/post/list/all/${blogId}`, {headers}).body)
        if(posts.length >= NB_POSTS) {
          console.log('Posts already created')
        } else {
          const nbPosts = NB_POSTS - posts.length
          const groups = {}
          groupIds.forEach(groupId => groups[groupId] = ["org-entcore-blog-controllers-PostController|list","org-entcore-blog-controllers-BlogController|get","org-entcore-blog-controllers-PostController|comments","org-entcore-blog-controllers-PostController|get","org-entcore-blog-controllers-PostController|updateComment","org-entcore-blog-controllers-PostController|deleteComment","org-entcore-blog-controllers-PostController|comment"])
          const sharePayload = {
            bookmarks: {},
            groups: groups
          };
          res = http.put(`${rootUrl}/blog/share/resource/${blogId}`, JSON.stringify(sharePayload), {headers: getHeaders(userSession)})
          check(res, {
            'share blog': (r) => r.status === 200
          })
          for(let i = 0; i < nbPosts; i++) {
            const postPayload = JSON.stringify({
              title: `Post ${String(i).padStart(5, '0')} de ${user.login}`,
              content: `Le contenu du super post ${String(i + posts.length).padStart(5, '0')} du blog de ${user.login}`,
            })
            res = http.post(`${rootUrl}/blog/post/${blogId}`, postPayload, {headers})
            check(res, {
              'create post': (r) => r.status === 200
            })
            const postId = JSON.parse(res.body)['_id']
            res = http.put(`${rootUrl}/blog/post/publish/${blogId}/${postId}`, {}, {headers})
            check(res, {
              'publish post': (r) => r.status === 200
            })
          }
        }
      }
    }
  })
}

export function activateUsers(structure, session) {
  describe("[Audience-Init] Activate users", () => {
    let res = http.get(`${rootUrl}/directory/structure/${structure.id}/users`, { headers: getHeaders(session) })
    check(res, {
      'fetch structure users': (r) => r.status == 200
    })
    const users = JSON.parse(res.body);
    for(let i = 0; i < users.length; i++) {
      const user = users[i]
      if(user.code) {
        const fd = {}
        fd['login'] = user.login
        fd['activationCode'] = user.code
        fd['password'] = 'password'
        fd['confirmPassword'] = 'password'
        fd['acceptCGU'] = 'true'
        res = http.post(`${rootUrl}/auth/activation`, fd, {follow: false, redirects: 0, headers: {Host: 'localhost'}})
        check(res, {
          'activate user': (r) => r.status === 302
        })
      }
    }
  })
}

export function linkRoleToUsers(structure, role, session) {
  describe("[Audience-Init] Link blog role to users of the structure", () => {
    const roles = getRolesOfStructure(structure.id, session);
    const teacherRoles = roles.filter(role => role.name === `Teachers from group ${structure.name}.` ||
                                              role.name === `Enseignants du groupe ${structure.name}.`)[0];
    if(teacherRoles.roles.indexOf(role.name) >= 0) {
      console.log('Role already attributed to teachers')
    } else {
      const headers = getHeaders(session);
      headers['content-type'] = 'application/json'
      const params = { headers }
      const payload = JSON.stringify({
        groupId: teacherRoles.id,
        roleIds: (teacherRoles.roles || []).concat([role.id])
      })
      const res = http.post(`${rootUrl}/appregistry/authorize/group?schoolId=${structure.id}`, payload, params)
      check(res, {
        'link role to structure': (r) => r.status == 200
      })
    }
  })
}

export function createStructure(schoolName, teachers, session) {
  let ecoleAudience = getSchoolByName(schoolName, session);
  if(ecoleAudience) {
    console.log("School already exists")
  } else {
    const fd = new FormData();
    fd.append('type', 'CSV');
    fd.append('structureName', schoolName);
    fd.append('Teacher', http.file(teachers, 'enseignants.csv'));
    const headers = getHeaders(session);
    headers['Content-Type'] = 'multipart/form-data; boundary=' + fd.boundary
    const params = { headers };
    describe("[Audience-Init] Import data", () => {
      const res = http.post(`${rootUrl}/directory/wizard/import`, fd.body(), params);
      check(res, {
        'import structure is ok': (r) => r.status == 200
      })
      ecoleAudience = getSchoolByName(schoolName)
    })
  }
  return ecoleAudience;
}
export function getRolesOfStructure(structureId, session) {
  let res = http.get(`${rootUrl}/appregistry/groups/roles?structureId=${structureId}`, {headers: getHeaders(session)});
  check(res, {
    'get structure roles should be ok': (r) => r.status == 200
  })
  return JSON.parse(res.body)
}

export function createAndSetRole(applicationName, session) {
  const roleName = `${applicationName} - All - Stress Test`;
  let role = getRoleByName(roleName, session);
  if(role) {
    console.log(`Role ${roleName} already existed`)
  } else {
    let res = http.get(`${rootUrl}/appregistry/applications/actions?actionType=WORKFLOW`, {headers: getHeaders(session)})
    check(res, { 'get workflow actions': (r) => r.status == 200 });
    const application = JSON.parse(res.body).filter(entry => entry.name === applicationName)[0];
    const actions = application.actions.map(entries => entries[0])
    const headers = getHeaders(session)
    headers['content-type'] = 'application/json'
    const payload = {
      role: roleName,
      actions
    }
    res = http.post(`${rootUrl}/appregistry/role`, JSON.stringify(payload), {headers})
    console.log(res)
    check(res, { 'save role ok': (r) => r.status == 201 });
    role = getRoleByName(roleName, session);
  }
  return role
}

export function getRoleByName(name, session) {
  let ecoles = http.get(`${rootUrl}/appregistry/roles`, {headers: getHeaders(session)})
  const result = JSON.parse(ecoles.body);
  return result.filter(role => role.name === name)[0]
}