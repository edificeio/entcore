import {
  authenticateWeb,
  initStructure,
  Session,
  Structure,
  getUsersOfSchool,
  getRandomUserWithProfile,
  Group,
  deleteGroupOrFail,
  getHeaders,
  createUserAndGetData,
  getProfileGroupOfStructureByType,
  createClassAndGetIdOrFail,
  getClassesOfStructureOrFail,
  createShareBookMarkOrFail,
  getShareBookMarkOrFail,
  getSearchCriteria,
  getUserPreferencesApi,
  createPositionOrFail,
  getPositionsOfStructure,
  getPositionByIdOrFail,
  searchPositions,
  deletePosition,
  UserPosition,
  UserProfileType,
  UserInfo,
  getSchoolByName,
  createGroupOrFail,
  addUsersToGroup
} from "../../../node_modules/edifice-k6-commons/dist/index.js";
import http from "k6/http";
import {check, group} from "k6";


const maxDuration = __ENV.MAX_DURATION || "20m";
const schoolName = __ENV.DATA_SCHOOL_NAME || "Directory";
const gracefulStop = parseInt(__ENV.GRACEFUL_STOP || "2s");
const rootUrl = __ENV.ROOT_URL;
const skipInit = __ENV.SKIP_INIT === "true";

const types: UserProfileType[] = ['Teacher', 'Relative', 'Student'];

export const options = {
  setupTimeout: "1h",
  thresholds: {
    checks: ["rate == 1.00"],
  },
  scenarios: {
    /*testClassEndpoints: {
      executor: "per-vu-iterations",
      exec: "testClassEndpoints",
      vus: 1,
      maxDuration: maxDuration,
      gracefulStop,
    },
    testStructureEndpoints: {
      executor: "per-vu-iterations",
      exec: "testStructureEndpoints",
      vus: 1,
      maxDuration: maxDuration,
      gracefulStop,
    },
    */
    testUserEndpoints: {
      executor: "per-vu-iterations",
      exec: "testUserEndpoints",
      vus: 1,
      maxDuration: maxDuration,
      gracefulStop,
    },
    testGroupEndpoints: {
      executor: "per-vu-iterations",
      exec: "testGroupEndpoints",
      vus: 1,
      maxDuration: maxDuration,
      gracefulStop,
    },
    testStructureEndpoints: {
      executor: "per-vu-iterations",
      exec: "testStructureEndpoints",
      vus: 1,
      maxDuration: maxDuration,
      gracefulStop,
    },
    testClassEndpoints: {
      executor: "per-vu-iterations",
      exec: "testClassEndpoints",
      vus: 1,
      maxDuration: maxDuration,
      gracefulStop,
    },
    testShareBookmarkEndpoints: {
      executor: "per-vu-iterations",
      exec: "testShareBookmarkEndpoints",
      vus: 1,
      maxDuration: maxDuration,
      gracefulStop,
    },
    testUserBookEndpoints: {
      executor: "per-vu-iterations",
      exec: "testUserBookEndpoints",
      vus: 1,
      maxDuration: maxDuration,
      gracefulStop,
    },
    testPositionEndpoints: {
      executor: "per-vu-iterations",
      exec: "testPositionEndpoints",
      vus: 1,
      maxDuration: maxDuration,
      gracefulStop,
    },
  },
};

type InitData = {
  structure: Structure;
  users: UserInfo[];
}

export function setup() {
  let structure1: Structure;
  let users: UserInfo[];

  group("[Directory] Initialize data", () => {
    authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);
    if (skipInit) {
      structure1 = getSchoolByName(schoolName);
    } else {
      structure1 = initStructure(`${schoolName}`);
    }
    users = getUsersOfSchool(structure1);
  });
  return { structure : structure1, users };
}

/*******************************************************************************************************
 *  User Endpoints
 ******************************************************************************************************/
export function testUserEndpoints(data: InitData) {

  group('[Directory] GET /user/:userId - Get user by id', () => {
    authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);
    for(const type of types) {
      const user = getRandomUserWithProfile(data.users, type);
      if(user) {
        const res = http.get(`${rootUrl}/directory/user/${user.id}`, { headers: getHeaders() });
        const body = JSON.parse(<string>res.body);
        check(res, {
          [`get user ${type} returns 200`]: (r) => r.status === 200,
          [`get user ${type} has id`]: (r) => body.id === user.id,
          [`get user ${type} has login`]: (r) => body.login === user.login,
          [`get user ${type} has one profile`]: (r) => body.profiles.length === 1,
          [`get user ${type} has right profile`]: (r) => body.profiles[0] === type,
          [`get user ${type} is in our structure`]: (r) => body.structures[0] === data.structure.externalId,
        });
        if(type == 'Relative') {
          check(res, {
          [`get user ${type} has children`]: (r) => body.children.length > 0,
          [`get user ${type} has child with data`]: (r) => body.children.every(child => !!child.id && !!child.displayName),
          })
        } else if(type == 'Student') {
          check(res, {
          [`get user ${type} has parents`]: (r) => body.parents.length > 0,
          [`get user ${type} has parents with data`]: (r) => body.parents.every(parent => !!parent.id && !!parent.displayName),
          })
        }
      } else {
        console.log(`No user with profile ${type} found, skipping GET /user/:userId test for this profile`);
      }
    }
  });

  group('[Directory] GET /user/:userId/groups - Get user groups', () => {
    authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);
    for (const type of types) {
      const user = getRandomUserWithProfile(data.users, type);
      let res = http.get(`${rootUrl}/directory/user/${user.id}/groups`, { headers: getHeaders() });
      const body = JSON.parse(<string>res.body);
      check(res, {
        [`get groups of ${type} returns 200`]: (r) => r.status === 200,
        [`get groups of ${type} is array`]: (r) => Array.isArray(body),
        [`get groups of ${type} has at least the structure group`]: (r) => body.some((g: Group) => g.subType === "StructureGroup"),
        [`get groups of ${type} has at least the structure group for the right structure`]: (r) => body.some((g: Group) => g.subType === "StructureGroup" && g.filter === type && g.structures.some(s => s.id === data.structure.id)),
        [`get groups of ${type} has at least a class group`]: (r) => body.some((g: Group) => g.subType === "ClassGroup" && g.structures.some(s => s.id === data.structure.id) && g.classes && g.classes.filter(c => !!c.id).length > 0),
      });
    }
  });

  group('[Directory] GET /myinfos - Get current user info', () => {
    for (const type of types) {
      const user = getRandomUserWithProfile(data.users, type);
      authenticateWeb(user.login);
      const res = http.get(`${rootUrl}/directory/myinfos`, { headers: getHeaders() });
      const body = JSON.parse(<string>res.body);
      check(res, {
        [`myinfos returns 200 for a ${type}`]: (r) => r.status === 200,
        [`myinfos has id for a ${type}`]: (r) => !!body.id,
        [`get user ${type} has id`]: (r) => body.id === user.id,
        [`get user ${type} has login`]: (r) => body.login === user.login,
        [`get user ${type} has one profile`]: (r) => body.profiles.length === 1,
        [`get user ${type} has right profile`]: (r) => body.profiles[0] === type,
        [`get user ${type} is in our structure`]: (r) => body.structures[0] === data.structure.externalId,
      });
    }
  });

  group('[Directory] GET /myclasses - Get current user classes', () => {
    const teacher = getRandomUserWithProfile(data.users, 'Teacher');
    authenticateWeb(teacher.login);
    let res = http.get(`${rootUrl}/directory/myclasses`, { headers: getHeaders() });
    const body = JSON.parse(<string>res.body);
    check(body, {
      'myclasses returns 200 for a teacher': (r) => res.status === 200,
      'has at least one class': (r) => r.schools && r.schools[0].classes.filter(c => !!c && !!c.id && !!c.name).length > 0,
    });
  });

  group('[Directory] GET /user/admin/list - List users admin', () => {
    authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);
    const res = http.get(`${rootUrl}/directory/user/admin/list?structureId=${data.structure.id}`, { headers: getHeaders() });
    const body = JSON.parse(<string>res.body);
    check(res, {
      'list admin users returns 200': (r) => r.status === 200,
      'list admin users is array': (r) => Array.isArray(body),
      'list admin users has entries': (r) => body.length > 0,
      'all users in the school are in admin list': (r) => {
        const adminUsersLogins = body.map((u: UserInfo) => u.login);
        return data.users.every(u => adminUsersLogins.includes(u.login));
      }
    });
  });

  group('[Directory] PUT /user/:userId - Update user', () => {
    authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);
    const users = getUsersOfSchool(data.structure);
    const teacher = getRandomUserWithProfile(users, 'Teacher');
    const headers = getHeaders();
    headers['content-type'] = 'application/json';
    const newDisplayName = "K6Updated_" + Date.now();
    const res = http.put(`${rootUrl}/directory/user/${teacher.id}`, JSON.stringify({ displayName: newDisplayName }), { headers });
    check(res, {
      'update user returns 200': (r) => r.status === 200,
    });
    // Verify the update was applied
    const verifyRes = http.get(`${rootUrl}/directory/user/${teacher.id}`, { headers: getHeaders() });
    const verifyBody = JSON.parse(<string>verifyRes.body);
    check(verifyRes, {
      'verify update user returns 200': (r) => r.status === 200,
      'verify displayName was updated': () => verifyBody.displayName === newDisplayName,
    });
  });
  group('[Directory] POST /user/function/:userId - Add function to user', () => {
    authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);
    const users = getUsersOfSchool(data.structure);
    const teacher = getRandomUserWithProfile(users, 'Teacher');
    const headers = getHeaders();
    headers['content-type'] = 'application/json';
    const payload = JSON.stringify({
      functionCode: "ADMIN_LOCAL",
      inherit: "s",
      scope: [data.structure.id],
    });
    const res = http.post(`${rootUrl}/directory/user/function/${teacher.id}`, payload, { headers });
    check(res, {
      'add function returns 200 or 201': (r) => r.status === 200 || r.status === 201,
    });
    // Verify the function was added
    const functionsRes = http.get(`${rootUrl}/directory/user/${teacher.id}/functions`, { headers: getHeaders() });
    const functions = JSON.parse(<string>functionsRes.body);
    check(functionsRes, {
      'verify list functions returns 200': (r) => r.status === 200,
      'verify ADMIN_LOCAL function was added': () => {
        const fn = functions[0].functions[0];
        return fn[0] === "ADMIN_LOCAL" && fn[1] && Array.isArray(fn[1]) && fn[1].includes(data.structure.id);
      },
    });
    // cleanup: remove function
    http.del(`${rootUrl}/directory/user/function/${teacher.id}/ADMIN_LOCAL`, null, { headers: getHeaders() });
    // Verify the function was removed
    const afterRemoveRes = http.get(`${rootUrl}/directory/user/${teacher.id}/functions`, { headers: getHeaders() });
    // [ { functions: [] } ]
    const afterRemoveFunctions = JSON.parse(<string>afterRemoveRes.body);
    check(afterRemoveRes, {
      'verify function removed returns 200': (r) => r.status === 200,
      'verify ADMIN_LOCAL function was removed': () => afterRemoveFunctions[0].functions.length === 0,
    });
  });
  group('[Directory] GET /user/:userId/functions - List user functions', () => {
    authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);
    const users = getUsersOfSchool(data.structure);
    const teacher = getRandomUserWithProfile(users, 'Teacher');
    const res = http.get(`${rootUrl}/directory/user/${teacher.id}/functions`, { headers: getHeaders() });
    check(res, {
      'list functions returns 200': (r) => r.status === 200,
      'list functions is array': (r) => Array.isArray(JSON.parse(<string>r.body)),
    });
  });

  group('[Directory] GET /user/:userId/children - List user children', () => {
    authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);
    const users = getUsersOfSchool(data.structure);
    const relative = getRandomUserWithProfile(users, 'Relative');
    const res = http.get(`${rootUrl}/directory/user/${relative.id}/children`, { headers: getHeaders() });
    //[ { structureName: "Directory", children: [ { classesNames: [ "TPS" ], displayName: "DELAUNAY Julien", externalId: "9720ea559073a94a6efea28873bef4e8d62bab7e", id: "fead333e-b0fc-4a6c-939f-74df9a4af696" } ] } ] 
    const body = JSON.parse(<string>res.body);
    check(res, {
      'list children returns 200': (r) => r.status === 200,
      'list children is not empty': () => Array.isArray(body) && body.length > 0,
      'list children has children info': () => body[0].children.every((c: any) => !!c.id && !!c.displayName),
    });
  });
  

  group('[Directory] GET /user/adml/list/:structureId - List ADML', () => {
    authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);
    let res = http.get(`${rootUrl}/directory/user/adml/list/${data.structure.id}`, { headers: getHeaders() });
    let body = JSON.parse(<string>res.body);
    check(res, {
      'list adml returns 200': (r) => r.status === 200,
      'list adml is array': (r) => Array.isArray(body),
      'list adml is empty': (r) => body.length === 0,
    });
    // Add an ADML function
    const teacher1 = getRandomUserWithProfile(data.users, 'Teacher');
    const teacher2 = getRandomUserWithProfile(data.users, 'Teacher', [teacher1]);
    const headers = getHeaders('application/json');
    const payload = JSON.stringify({
      functionCode: "ADMIN_LOCAL",
      inherit: "s",
      scope: [data.structure.id],
    });
    for(let teacher of [teacher1, teacher2]) {
      http.post(`${rootUrl}/directory/user/function/${teacher.id}`, payload, { headers });
    }

    res = http.get(`${rootUrl}/directory/user/adml/list/${data.structure.id}`, { headers: getHeaders() });

    body = JSON.parse(<string>res.body);
    check(res, {
      'list adml returns 200': (r) => r.status === 200,
      'list adml is not empty': (r) => body.length === 2,
      'list adml has the user with the function we just added': () => body.some((u: any) => u.id === teacher1.id && u.login === teacher1.login && u.type === "Teacher") &&
        body.some((u: any) => u.id === teacher2.id && u.login === teacher2.login && u.type === "Teacher"),
    });
    
    // cleanup: remove function
    for(let teacher of [teacher1, teacher2]) {
      http.del(`${rootUrl}/directory/user/function/${teacher.id}/ADMIN_LOCAL`, null, { headers: getHeaders() });
    }
  });

  group('[Directory] GET /duplicates - List duplicates', () => {
    authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);
    const res = http.get(`${rootUrl}/directory/duplicates?structure=${data.structure.id}`, { headers: getHeaders() });
    check(res, {
      'list duplicates returns 200': (r) => r.status === 200,
      'list duplicates is array': (r) => Array.isArray(JSON.parse(<string>r.body)),
    });
  });
  group('[Directory] GET /list/isolated - List isolated users', () => {
    authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);
    const res = http.get(`${rootUrl}/directory/list/isolated?structureId=${data.structure.id}`, { headers: getHeaders() });
    check(res, {
      'list isolated returns 200': (r) => r.status === 200,
      'list isolated is array': (r) => Array.isArray(JSON.parse(<string>r.body)),
      'no isolated users': (r) => JSON.parse(<string>r.body).length === 0,
    });
  });
  group('[Directory] GET /userbook/moods - Get moods', () => {
    authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);
    const res = http.get(`${rootUrl}/directory/userbook/moods`, { headers: getHeaders() });
    const body = JSON.parse(<string>res.body);
    check(res, {
      'get moods returns 200': (r) => r.status === 200,
      'get moods is array': (r) => Array.isArray(body),
      "get moods is as expected": (r) => {
        const expected = ["default","happy","proud","dreamy","love","tired","angry","worried","sick","joker","sad"];
        return Array.isArray(body) && body.length === expected.length && expected.every(m => body.includes(m));
      }
    });
  });

  group('[Directory] GET /user/mailstate - Get mail state', () => {
    authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);
    const users = getUsersOfSchool(data.structure);
    const teacher = getRandomUserWithProfile(users, 'Teacher');
    authenticateWeb(teacher.login);
    const res = http.get(`${rootUrl}/directory/user/mailstate`, { headers: getHeaders() });
    check(res, {
      'get mailstate returns 200': (r) => r.status === 200,
    });
  });

  group('[Directory] GET /user/mobilestate - Get mobile state', () => {
    authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);
    const users = getUsersOfSchool(data.structure);
    const teacher = getRandomUserWithProfile(users, 'Teacher');
    authenticateWeb(teacher.login);
    const res = http.get(`${rootUrl}/directory/user/mobilestate`, { headers: getHeaders() });
    check(res, {
      'get mobilestate returns 200': (r) => r.status === 200,
    });
  });

  group('[Directory] POST /directory/api/user - Create user', () => {
    authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);
    const firstName = "TestK6_" + Date.now();
    const user = createUserAndGetData({
      firstName: firstName,
      lastName: "UserCreated",
      type: "Personnel",
      structureId: data.structure.id,
      birthDate: "1990-01-01",
      positionIds: [],
    });
    check(user, {
      'created user has id': (u) => !!u.id,
      'created user has correct firstName': (u) => u.firstName === firstName,
    });

    // call myinfos with the new user to verify it works and returns the correct data
    const res = http.get(`${rootUrl}/directory/user/${user.id}`, { headers: getHeaders() });
    const body = JSON.parse(<string>res.body);
    check(res, {
      [`get user after creation returns 200`]: (r) => r.status === 200,
      [`get user after creation has id`]: (r) => body.id === user.id,
      [`get user after creation has login`]: (r) => body.login === user.login,
      [`get user after creation has one profile`]: (r) => body.profiles.length === 1,
      [`get user after creation has right profile`]: (r) => body.profiles[0] === 'Personnel',
      [`get user after creation is in our structure`]: (r) => body.structures[0] === data.structure.externalId,
    });

    // cleanup: delete created user
    http.del(`${rootUrl}/directory/user?userId=${user.id}`, null, { headers: getHeaders() });

    // now call myinfos again to verify the user is really deleted and we get an error or empty response
    const afterDeleteRes = http.get(`${rootUrl}/directory/user/${user.id}`, { headers: getHeaders() });
    check(afterDeleteRes, {
      'myinfos after delete returns 200': (r) => r.status === 200,
      [`get user after delete has no id`]: (r) => !JSON.parse(<string>r.body).id,
    });
  });

  group('[Directory] PUT /userbook/:userId - Update userbook', () => {
    authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);
    const teacher = getRandomUserWithProfile(data.users, 'Teacher');
    const headers = getHeaders('application/json');
    const newMotto = "k6_motto_" + Date.now();
    const res = http.put(`${rootUrl}/directory/userbook/${teacher.id}`, JSON.stringify({ motto: newMotto }), { headers });
    check(res, {
      'update userbook returns 200': (r) => r.status === 200,
    });
    // Verify the update was applied
    const verifyRes = http.get(`${rootUrl}/directory/userbook/${teacher.id}`, { headers: getHeaders() });
    const verifyBody = JSON.parse(<string>verifyRes.body);
    check(verifyRes, {
      'verify get userbook returns 200': (r) => r.status === 200,
      'verify motto was updated': () => verifyBody.motto === newMotto,
    });
  });

  group('[Directory] GET /userbook/:userId - Get userbook', () => {
    authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);
    const users = getUsersOfSchool(data.structure);
    const teacher = getRandomUserWithProfile(users, 'Teacher');
    const res = http.get(`${rootUrl}/directory/userbook/${teacher.id}`, { headers: getHeaders() });
    check(res, {
      'get userbook returns 200': (r) => r.status === 200,
    });
  });

  group('[Directory] GET /export/users - Export users', () => {
    authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);
    for(let type of types) {
      const res = http.get(`${rootUrl}/directory/export/users?structureId=${data.structure.id}&profile=${type}`, { headers: getHeaders() });
      check(res, {
        [`export users returns 200 for profile ${type}`]: (r) => r.status === 200,
        [`export users for profile ${type} contains all users of this type`]: (r) => {
          const exportedUsersLogin = JSON.parse(<string>r.body).map((u: any) => u.login);
          const usersOfType = data.users.filter(u => u.type === type);
          return usersOfType.every(u => exportedUsersLogin.includes(u.login));
        },
      });
    }
  });
}

/*******************************************************************************************************
 *  Group Endpoints
 ******************************************************************************************************/
export function testGroupEndpoints(data: InitData) {

  group('[Directory] GET /group/admin/list - List groups admin', () => {
    authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);
    const res = http.get(`${rootUrl}/directory/group/admin/list?structureId=${data.structure.id}`, { headers: getHeaders() });
    const body = JSON.parse(<string>res.body);
    check(res, {
      'list groups admin returns 200': (r) => r.status === 200,
      'list groups admin is array': (r) => Array.isArray(body),
      'list groups admin has at least 50 entries': (r) => body.length >= 50,
      'list groups admin groups all have id, name and filter': (r) => body.every((g: any) => !!g.id && !!g.name && !!g.filter),
    });
  });

  group('[Directory] POST/PUT/DELETE /group - CRUD manual group', () => {
    authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);

    // Create group
    const createdGroup: Group = createGroupOrFail("k6-test-group-crud", data.structure);
    check(createdGroup, {
      'created group has id': (g) => !!g.id,
      'created group has name': (g) => g.name === "k6-test-group-crud",
    });

    // Update group
    const headers = getHeaders();
    headers['content-type'] = 'application/json';
    const updateRes = http.put(`${rootUrl}/directory/group/${createdGroup.id}`, JSON.stringify({ name: "k6-test-group-updated" }), { headers });
    check(updateRes, {
      'update group returns 200': (r) => r.status === 200,
    });

    // Get group
    const getRes = http.get(`${rootUrl}/directory/group/${createdGroup.id}`, { headers: getHeaders() });
    check(getRes, {
      'get group returns 200': (r) => r.status === 200,
      'get group has updated name': (r) => JSON.parse(<string>r.body).name === "k6-test-group-updated",
    });

    // Delete group
    deleteGroupOrFail(createdGroup);
  });

  group('[Directory] PUT /group/:groupId/users/add - Add users to group', () => {
    authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);

    const users = getUsersOfSchool(data.structure);
    const teacher = getRandomUserWithProfile(users, 'Teacher');
    const grp: Group = createGroupOrFail("k6-test-add-users-group", data.structure);

    const headers = getHeaders('application/json');
    const res = http.put(`${rootUrl}/directory/group/${grp.id}/users/add`, JSON.stringify({ userIds: [teacher.id] }), { headers });
    check(res, {
      'add users to group returns 200': (r) => r.status === 200,
    });
    // Verify user was added to the group
    const verifyRes = http.get(`${rootUrl}/directory/user/admin/list?groupId=${grp.id}`, { headers: getHeaders() });
    const groupUsers = JSON.parse(<string>verifyRes.body);
    check(verifyRes, {
      'verify group users returns 200': (r) => r.status === 200,
      'verify user is in group': () => groupUsers.some((u: any) => u.id === teacher.id),
    });

    // cleanup
    deleteGroupOrFail(grp);
  });

  group('[Directory] PUT /group/:groupId/users/delete - Remove users from group', () => {
    authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);

    const users = getUsersOfSchool(data.structure);
    const teacher = getRandomUserWithProfile(users, 'Teacher');
    const grp: Group = createGroupOrFail("k6-test-remove-users-group", data.structure);
    addUsersToGroup([teacher.id], grp);

    const headers = getHeaders('application/json');
    const res = http.put(`${rootUrl}/directory/group/${grp.id}/users/delete`, JSON.stringify({ userIds: [teacher.id] }), { headers });
    check(res, {
      'remove users from group returns 200': (r) => r.status === 200,
    });
    // Verify user was removed from the group
    const verifyRes = http.get(`${rootUrl}/directory/user/admin/list?groupId=${grp.id}`, { headers: getHeaders() });
    const groupUsers = JSON.parse(<string>verifyRes.body);
    check(verifyRes, {
      'verify group users after remove returns 200': (r) => r.status === 200,
      'verify user is no longer in group': () => !groupUsers.some((u: any) => u.id === teacher.id),
    });

    // cleanup
    deleteGroupOrFail(grp);
  });

  group('[Directory] GET /group/admin/funcAndDisciplines - List func and disciplines groups', () => {
    authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);
    const res = http.get(`${rootUrl}/directory/group/admin/funcAndDisciplines?structureId=${data.structure.id}`, { headers: getHeaders() });
    check(res, {
      'list func and disciplines returns 200': (r) => r.status === 200,
      'list func and disciplines is array': (r) => Array.isArray(JSON.parse(<string>r.body)),
    });
  });

  group('[Directory] GET /group/communityGroup - Get community group', () => {
    authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);
    const res = http.get(`${rootUrl}/directory/group/communityGroup?structureId=${data.structure.id}`, { headers: getHeaders() });
    check(res, {
      'get community group returns 200': (r) => r.status === 200,
      'get community group is array': (r) => Array.isArray(JSON.parse(<string>r.body)),
    });
  });
}

/*******************************************************************************************************
 *  Structure Endpoints
 ******************************************************************************************************/
export function testStructureEndpoints(data: InitData) {

  group('[Directory] GET /structure/admin/list - List structures admin', () => {
    authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);
    const res = http.get(`${rootUrl}/directory/structure/admin/list`, { headers: getHeaders() });
    check(res, {
      'list structures admin returns 200': (r) => r.status === 200,
      'list structures admin is array': (r) => Array.isArray(JSON.parse(<string>r.body)),
      'list structures admin has entries': (r) => JSON.parse(<string>r.body).length > 0,
    });
  });

  group('[Directory] GET /structures - List all structures', () => {
    authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);
    const res = http.get(`${rootUrl}/directory/structures`, { headers: getHeaders() });
    check(res, {
      'list all structures returns 200': (r) => r.status === 200,
      'list all structures is array': (r) => Array.isArray(JSON.parse(<string>r.body)),
    });
  });

  group('[Directory] GET /structure/:structureId/children - List structure children', () => {
    authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);
    const res = http.get(`${rootUrl}/directory/structure/${data.structure.id}/children`, { headers: getHeaders() });
    check(res, {
      'list structure children returns 200': (r) => r.status === 200,
      'list structure children is array': (r) => Array.isArray(JSON.parse(<string>r.body)),
    });
  });

  group('[Directory] GET /structure/:structureId/levels - Get structure levels', () => {
    authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);
    const res = http.get(`${rootUrl}/directory/structure/${data.structure.id}/levels`, { headers: getHeaders() });
    check(res, {
      'get structure levels returns 200': (r) => r.status === 200,
      'get structure levels is array': (r) => Array.isArray(JSON.parse(<string>r.body)),
    });
  });

  group('[Directory] GET /structure/:id/users - List structure users', () => {
    authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);
    const res = http.get(`${rootUrl}/directory/structure/${data.structure.id}/users`, { headers: getHeaders() });
    check(res, {
      'list structure users returns 200': (r) => r.status === 200,
      'list structure users is array': (r) => Array.isArray(JSON.parse(<string>r.body)),
      'list structure users has entries': (r) => JSON.parse(<string>r.body).length > 0,
    });
  });

  group('[Directory] GET /structure/:id/removedUsers - List removed users', () => {
    authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);
    const res = http.get(`${rootUrl}/directory/structure/${data.structure.id}/removedUsers`, { headers: getHeaders() });
    check(res, {
      'list removed users returns 200': (r) => r.status === 200,
      'list removed users is array': (r) => Array.isArray(JSON.parse(<string>r.body)),
    });
  });

  group('[Directory] GET /structure/:structureId/metrics - Get structure metrics', () => {
    authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);
    const res = http.get(`${rootUrl}/directory/structure/${data.structure.id}/metrics`, { headers: getHeaders() });
    check(res, {
      'get structure metrics returns 200': (r) => r.status === 200,
    });
  });

  group('[Directory] GET /structure/:id/sources - List sources', () => {
    authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);
    const res = http.get(`${rootUrl}/directory/structure/${data.structure.id}/sources`, { headers: getHeaders() });
    check(res, {
      'list sources returns 200': (r) => r.status === 200,
      'list sources is array': (r) => Array.isArray(JSON.parse(<string>r.body)),
    });
  });

  group('[Directory] GET /structure/:id/aaffunctions - List AAF functions', () => {
    authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);
    const res = http.get(`${rootUrl}/directory/structure/${data.structure.id}/aaffunctions`, { headers: getHeaders() });
    check(res, {
      'list aaf functions returns 200': (r) => r.status === 200,
      'list aaf functions is array': (r) => Array.isArray(JSON.parse(<string>r.body)),
    });
  });

  group('[Directory] GET /structure/:id/quicksearch/users - Quick search users', () => {
    authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);
    const users = getUsersOfSchool(data.structure);
    const teacher = getRandomUserWithProfile(users, 'Teacher');
    const searchTerm = teacher.lastName.substring(0, 3);
    const res = http.get(`${rootUrl}/directory/structure/${data.structure.id}/quicksearch/users?input=${searchTerm}`, { headers: getHeaders() });
    check(res, {
      'quick search users returns 200': (r) => r.status === 200,
      'quick search users is array': (r) => Array.isArray(JSON.parse(<string>r.body)),
    });
  });

  group('[Directory] PUT /structure/:structureId - Update structure', () => {
    authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);
    const headers = getHeaders();
    headers['content-type'] = 'application/json';
    const updatedName = data.structure.name + "_updated";
    const res = http.put(`${rootUrl}/directory/structure/${data.structure.id}`, JSON.stringify({ name: updatedName }), { headers });
    check(res, {
      'update structure returns 200': (r) => r.status === 200,
    });
    // Verify the update was applied
    const verifyRes = http.get(`${rootUrl}/directory/structure/admin/list`, { headers: getHeaders() });
    const structures = JSON.parse(<string>verifyRes.body);
    const updatedStructure = structures.find((s: any) => s.id === data.structure.id);
    check(verifyRes, {
      'verify structure list returns 200': (r) => r.status === 200,
      'verify structure name was updated': () => updatedStructure && updatedStructure.name === updatedName,
    });
    // Restore original name
    http.put(`${rootUrl}/directory/structure/${data.structure.id}`, JSON.stringify({ name: data.structure.name }), { headers });
  });

  group('[Directory] GET /structure/:structureId/massMail/users - Get massmail users', () => {
    authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);
    const res = http.get(`${rootUrl}/directory/structure/${data.structure.id}/massMail/users?p=Teacher`, { headers: getHeaders() });
    check(res, {
      'get massmail users returns 200': (r) => r.status === 200,
      'get massmail users is array': (r) => Array.isArray(JSON.parse(<string>r.body)),
    });
  });

  group('[Directory] GET /structure/:structureId/massMail/allUsers - Get all massmail users', () => {
    authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);
    const res = http.get(`${rootUrl}/directory/structure/${data.structure.id}/massMail/allUsers`, { headers: getHeaders() });
    check(res, {
      'get all massmail users returns 200': (r) => r.status === 200,
      'get all massmail users is array': (r) => Array.isArray(JSON.parse(<string>r.body)),
    });
  });

  group('[Directory] PUT /structure/:structureId/link/:userId - Link user to structure', () => {
    authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);
    const users = getUsersOfSchool(data.structure);
    const teacher = getRandomUserWithProfile(users, 'Teacher');
    // Re-link already linked user to verify the endpoint works
    const res = http.put(`${rootUrl}/directory/structure/${data.structure.id}/link/${teacher.id}`, null, { headers: getHeaders() });
    check(res, {
      'link user to structure returns 200': (r) => r.status === 200,
    });
  });
}

/*******************************************************************************************************
 *  Class Endpoints
 ******************************************************************************************************/
export function testClassEndpoints(data: InitData) {

  group('[Directory] GET /class/admin/list - List classes admin', () => {
    authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);
    const res = http.get(`${rootUrl}/directory/class/admin/list?structureId=${data.structure.id}`, { headers: getHeaders() });
    check(res, {
      'list classes admin returns 200': (r) => r.status === 200,
      'list classes admin is array': (r) => Array.isArray(JSON.parse(<string>r.body)),
    });
  });

  group('[Directory] POST /class/:structureId - Create class', () => {
    authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);
    const classId = createClassAndGetIdOrFail(data.structure.id, "k6-test-class");
    check(classId, {
      'created class has id': (id) => !!id,
    });

    // GET /class/:classId
    const getRes = http.get(`${rootUrl}/directory/class/${classId}`, { headers: getHeaders() });
    check(getRes, {
      'get class returns 200': (r) => r.status === 200,
      'get class has name': (r) => JSON.parse(<string>r.body).name === "k6-test-class",
    });
  });

  group('[Directory] PUT /class/:classId - Update class', () => {
    authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);
    const classId = createClassAndGetIdOrFail(data.structure.id, "k6-class-to-update");
    const headers = getHeaders();
    headers['content-type'] = 'application/json';
    const res = http.put(`${rootUrl}/directory/class/${classId}`, JSON.stringify({ name: "k6-class-updated" }), { headers });
    check(res, {
      'update class returns 200': (r) => r.status === 200,
    });
    // Verify the update was applied
    const verifyRes = http.get(`${rootUrl}/directory/class/${classId}`, { headers: getHeaders() });
    const verifyBody = JSON.parse(<string>verifyRes.body);
    check(verifyRes, {
      'verify get class returns 200': (r) => r.status === 200,
      'verify class name was updated': () => verifyBody.name === "k6-class-updated",
    });
  });

  group('[Directory] GET /class/:classId/users - Find class users', () => {
    authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);
    const classes = getClassesOfStructureOrFail(data.structure.id);
    if (classes.length > 0) {
      const classId = classes[0].id;
      const res = http.get(`${rootUrl}/directory/class/${classId}/users`, { headers: getHeaders() });
      check(res, {
        'find class users returns 200': (r) => r.status === 200,
        'find class users is array': (r) => Array.isArray(JSON.parse(<string>r.body)),
      });
    }
  });

  group('[Directory] PUT /class/:classId/link/:userId - Link user to class', () => {
    authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);
    const classId = createClassAndGetIdOrFail(data.structure.id, "k6-class-link-user");
    const users = getUsersOfSchool(data.structure);
    const teacher = getRandomUserWithProfile(users, 'Teacher');
    const res = http.put(`${rootUrl}/directory/class/${classId}/link/${teacher.id}`, null, { headers: getHeaders() });
    check(res, {
      'link user to class returns 200': (r) => r.status === 200,
    });
    // Verify user is now in the class
    const verifyRes = http.get(`${rootUrl}/directory/class/${classId}/users`, { headers: getHeaders() });
    const classUsers = JSON.parse(<string>verifyRes.body);
    check(verifyRes, {
      'verify class users returns 200': (r) => r.status === 200,
      'verify user is in class': () => classUsers.some((u: any) => u.id === teacher.id),
    });
  });

  group('[Directory] DELETE /class/:classId/unlink/:userId - Unlink user from class', () => {
    authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);
    const classId = createClassAndGetIdOrFail(data.structure.id, "k6-class-unlink-user");
    const users = getUsersOfSchool(data.structure);
    const teacher = getRandomUserWithProfile(users, 'Teacher');
    // First link, then unlink
    http.put(`${rootUrl}/directory/class/${classId}/link/${teacher.id}`, null, { headers: getHeaders() });
    const res = http.del(`${rootUrl}/directory/class/${classId}/unlink/${teacher.id}`, null, { headers: getHeaders() });
    check(res, {
      'unlink user from class returns 200': (r) => r.status === 200,
    });
    // Verify user is no longer in the class
    const verifyRes = http.get(`${rootUrl}/directory/class/${classId}/users`, { headers: getHeaders() });
    const classUsers = JSON.parse(<string>verifyRes.body);
    check(verifyRes, {
      'verify class users after unlink returns 200': (r) => r.status === 200,
      'verify user is not in class': () => !classUsers.some((u: any) => u.id === teacher.id),
    });
  });

  group('[Directory] GET /class/users/detached - List detached users', () => {
    authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);
    const users = getUsersOfSchool(data.structure);
    const teacher = getRandomUserWithProfile(users, 'Teacher');
    authenticateWeb(teacher.login);
    const res = http.get(`${rootUrl}/directory/class/users/detached?structureId=${data.structure.id}`, { headers: getHeaders() });
    check(res, {
      'list detached users returns 200': (r) => r.status === 200,
    });
  });

  group('[Directory] POST /class/:classId/user - Create user in class', () => {
    authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);
    const classId = createClassAndGetIdOrFail(data.structure.id, "k6-class-create-user");
    const headers = getHeaders();
    headers['content-type'] = 'application/json';
    const res = http.post(`${rootUrl}/directory/class/${classId}/user`, JSON.stringify({
      firstName: "K6ClassUser",
      lastName: "Test",
      type: "Student",
      birthDate: "2010-05-15",
    }), { headers });
    check(res, {
      'create user in class returns 201': (r) => r.status === 201,
      'created user has id': (r) => !!JSON.parse(<string>r.body).id,
    });
  });
}

/*******************************************************************************************************
 *  ShareBookmark Endpoints
 ******************************************************************************************************/
export function testShareBookmarkEndpoints(data: InitData) {

  group('[Directory] POST/GET/PUT/DELETE /sharebookmark - CRUD share bookmark', () => {
    authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);
    const users = getUsersOfSchool(data.structure);
    const teacher1 = getRandomUserWithProfile(users, 'Teacher');
    const teacher2 = getRandomUserWithProfile(users, 'Teacher', [teacher1]);

    // Create
    const bookmark = createShareBookMarkOrFail({ name: "k6-bookmark", members: [teacher1.id, teacher2.id] });
    check(bookmark, {
      'created bookmark has id': (b) => !!b.id,
      'created bookmark has name': (b) => b.name === "k6-bookmark",
    });

    // Get by id
    const fetched = getShareBookMarkOrFail(bookmark.id);
    check(fetched, {
      'fetched bookmark has correct id': (b) => b.id === bookmark.id,
    });

    // Get all
    const allRes = http.get(`${rootUrl}/directory/sharebookmark/all`, { headers: getHeaders() });
    check(allRes, {
      'get all bookmarks returns 200': (r) => r.status === 200,
      'get all bookmarks is array': (r) => Array.isArray(JSON.parse(<string>r.body)),
    });

    // Update
    const headers = getHeaders();
    headers['content-type'] = 'application/json';
    const updateRes = http.put(`${rootUrl}/directory/sharebookmark/${bookmark.id}`, JSON.stringify({ name: "k6-bookmark-updated", members: [teacher1.id] }), { headers });
    check(updateRes, {
      'update bookmark returns 200': (r) => r.status === 200,
    });
    // Verify the update was applied
    const verifiedBookmark = getShareBookMarkOrFail(bookmark.id);
    check(verifiedBookmark, {
      'verify bookmark name was updated': (b) => b.name === "k6-bookmark-updated",
    });

    // Delete
    const delRes = http.del(`${rootUrl}/directory/sharebookmark/${bookmark.id}`, null, { headers: getHeaders() });
    check(delRes, {
      'delete bookmark returns 200': (r) => r.status === 200,
    });
  });
}

/*******************************************************************************************************
 *  UserBook Endpoints
 ******************************************************************************************************/
export function testUserBookEndpoints(data: InitData) {

  group('[Directory] GET /userbook/api/search - Search users in userbook', () => {
    authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);
    const users = getUsersOfSchool(data.structure);
    const teacher = getRandomUserWithProfile(users, 'Teacher');
    authenticateWeb(teacher.login);
    const res = http.get(`${rootUrl}/userbook/api/search?name=${teacher.lastName.substring(0, 3)}`, { headers: getHeaders() });
    check(res, {
      'search userbook returns 200': (r) => r.status === 200,
      'search userbook is array': (r) => Array.isArray(JSON.parse(<string>r.body)),
    });
  });

  group('[Directory] GET /userbook/api/person - Get person info', () => {
    authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);
    const users = getUsersOfSchool(data.structure);
    const teacher = getRandomUserWithProfile(users, 'Teacher');
    authenticateWeb(teacher.login);
    const res = http.get(`${rootUrl}/userbook/api/person`, { headers: getHeaders() });
    check(res, {
      'get person returns 200': (r) => r.status === 200,
      'get person has result': (r) => !!JSON.parse(<string>r.body).result,
    });
  });

  group('[Directory] GET /userbook/structures - Show user structures', () => {
    authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);
    const users = getUsersOfSchool(data.structure);
    const teacher = getRandomUserWithProfile(users, 'Teacher');
    authenticateWeb(teacher.login);
    const res = http.get(`${rootUrl}/userbook/structures`, { headers: getHeaders() });
    check(res, {
      'show structures returns 200': (r) => r.status === 200,
      'show structures is array': (r) => Array.isArray(JSON.parse(<string>r.body)),
      'show structures has entries': (r) => JSON.parse(<string>r.body).length > 0,
    });
  });

  group('[Directory] GET /userbook/structure/:structId - Show structure detail', () => {
    authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);
    const users = getUsersOfSchool(data.structure);
    const teacher = getRandomUserWithProfile(users, 'Teacher');
    authenticateWeb(teacher.login);
    const res = http.get(`${rootUrl}/userbook/structure/${data.structure.id}`, { headers: getHeaders() });
    check(res, {
      'show structure detail returns 200': (r) => r.status === 200,
      'show structure detail has users': (r) => !!JSON.parse(<string>r.body).users,
    });
  });

  group('[Directory] GET /userbook/visible/users/:groupId - Visible users of group', () => {
    authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);
    const users = getUsersOfSchool(data.structure);
    const teacher = getRandomUserWithProfile(users, 'Teacher');
    authenticateWeb(teacher.login);
    const profileGroup = getProfileGroupOfStructureByType('Teacher', data.structure);
    if (profileGroup) {
      const res = http.get(`${rootUrl}/userbook/visible/users/${profileGroup.id}`, { headers: getHeaders() });
      check(res, {
        'visible users of group returns 200': (r) => r.status === 200,
        'visible users of group is array': (r) => Array.isArray(JSON.parse(<string>r.body)),
      });
    }
  });

  group('[Directory] GET /userbook/search/criteria - Get search criteria', () => {
    authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);
    const users = getUsersOfSchool(data.structure);
    const teacher = getRandomUserWithProfile(users, 'Teacher');
    authenticateWeb(teacher.login);
    const criteria = getSearchCriteria();
    check(criteria, {
      'search criteria has structures': (c) => Array.isArray(c.structures),
      'search criteria has profiles': (c) => Array.isArray(c.profiles),
    });
  });

  group('[Directory] GET /userbook/person/birthday - Get birthdays', () => {
    authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);
    const users = getUsersOfSchool(data.structure);
    const teacher = getRandomUserWithProfile(users, 'Teacher');
    authenticateWeb(teacher.login);
    const res = http.get(`${rootUrl}/userbook/person/birthday`, { headers: getHeaders() });
    check(res, {
      'get birthdays returns 200': (r) => r.status === 200,
      'get birthdays is array': (r) => Array.isArray(JSON.parse(<string>r.body)),
    });
  });

  group('[Directory] GET/PUT /userbook/preference/:application - Get/Set preferences', () => {
    authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);
    const users = getUsersOfSchool(data.structure);
    const teacher = getRandomUserWithProfile(users, 'Teacher');
    authenticateWeb(teacher.login);

    // Put preference
    const headers = getHeaders();
    const prefValue = JSON.stringify({ key: "value_" + Date.now() });
    const putRes = http.put(`${rootUrl}/userbook/preference/k6test`, prefValue, { headers });
    check(putRes, {
      'put preference returns 200': (r) => r.status === 200,
    });

    // Get preference and verify it was set
    const getRes = http.get(`${rootUrl}/userbook/preference/k6test`, { headers: getHeaders() });
    const prefBody = JSON.parse(<string>getRes.body);
    check(getRes, {
      'get preference returns 200': (r) => r.status === 200,
      'verify preference was set': () => prefBody.preference === prefValue,
    });
  });

  group('[Directory] GET/PUT /userbook/api/preferences - V1 Preferences API', () => {
    authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);
    const users = getUsersOfSchool(data.structure);
    const teacher = getRandomUserWithProfile(users, 'Teacher');
    authenticateWeb(teacher.login);

    // Get preferences
    const prefs = getUserPreferencesApi();
    check(prefs, {
      'get preferences api returns object': (p) => typeof p === 'object',
    });
  });

  group('[Directory] GET /userbook/api/class - Get my class', () => {
    authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);
    const users = getUsersOfSchool(data.structure);
    const teacher = getRandomUserWithProfile(users, 'Teacher');
    authenticateWeb(teacher.login);
    const res = http.get(`${rootUrl}/userbook/api/class`, { headers: getHeaders() });
    check(res, {
      'get my class returns 200': (r) => r.status === 200,
      'get my class is array': (r) => Array.isArray(JSON.parse(<string>r.body)),
    });
  });
}

/*******************************************************************************************************
 *  Position Endpoints
 ******************************************************************************************************/
export function testPositionEndpoints(data: InitData) {

  group('[Directory] POST/GET/PUT/DELETE /positions - CRUD positions', () => {
    authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);

    // Create position
    const position: UserPosition = createPositionOrFail("k6-test-position", data.structure);
    check(position, {
      'created position has id': (p) => !!p.id,
      'created position has name': (p) => p.name === "k6-test-position",
    });

    // Get position by id
    const fetched = getPositionByIdOrFail(position.id);
    check(fetched, {
      'fetched position has correct id': (p) => p.id === position.id,
      'fetched position has correct name': (p) => p.name === "k6-test-position",
    });

    // List positions of structure
    const positions = getPositionsOfStructure(data.structure);
    check(positions, {
      'list positions is array': (p) => Array.isArray(p),
      'list positions contains created': (p) => p.some((pos: UserPosition) => pos.id === position.id),
    });

    // Search positions
    const searchRes = searchPositions("k6-test");
    check(searchRes, {
      'search positions returns 200': (r) => r.status === 200,
    });

    // Delete position
    const delRes = deletePosition(position.id);
    check(delRes, {
      'delete position returns 200 or 204': (r) => r.status === 200 || r.status === 204,
    });
  });
}