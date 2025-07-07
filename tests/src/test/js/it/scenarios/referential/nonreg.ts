import { check, fail } from "k6";
import http from "k6/http";
import {chai, describe } from "https://jslib.k6.io/k6chaijs/4.3.4.0/index.js";
import {
  authenticateWeb,
  initStructure,
  getRolesOfStructure,
  getUsersOfSchool,
  getRandomUserWithProfile,
  getHeaders
} from "../../../node_modules/edifice-k6-commons/dist/index.js";

const rootUrl = __ENV.ROOT_URL;

chai.config.logFailures = true;


export const options = {
  setupTimeout: "1h",
  maxRedirects: 0,
  thresholds: {
    checks: ["rate == 1.00"],
  },
  scenarios: {
    searchVisibles: {
      exec: 'testSearchVisibles',
      executor: "per-vu-iterations",
      vus: 1,
      maxDuration: "5s",
      gracefulStop: '1s',
    },
    communicationLink: {
      exec: 'testCommunicationLink',
      executor: "per-vu-iterations",
      vus: 1,
      maxDuration: "5s",
      gracefulStop: '1s',
    },
    groups: {
      exec: 'testGroups',
      executor: "per-vu-iterations",
      vus: 1,
      maxDuration: "5s",
      gracefulStop: '1s',
    },
    rules: {
      exec: 'testRules',
      executor: "per-vu-iterations",
      vus: 1,
      maxDuration: "5s",
      gracefulStop: '1s',
    },
    discover: {
      exec: 'tesDiscover',
      executor: "per-vu-iterations",
      vus: 1,
      maxDuration: "5s",
      gracefulStop: '1s',
    },
  },
};

/**
 * @returns A test dataset containing
 * - structure: a standalone structure
 * - adml: an ADML on this structure
 * - structureTree:
 *    - head: a head structure containing the following 2
 *    - headAdml: ADML of the head structure
 *    - structures: a list structures depending on the head structure
 *    - admls: a list of adml (one per structure and they are in the same order of the structures)
 */
export function setup() {
  let structure;
  let structure2;
  describe("[Referential-NoReg] Initialize data", () => {
    //////////////////////////////////////////////////////
    // Create a simple structure and create one ADML for it
    authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);
    structure = initStructure(`NoReg - Referential`, 'tiny');
    structure2 = initStructure(`NoReg - Referential - 2`, 'tiny');
  });
  return {structure, otherStructure: structure2};
}

export function testCommunicationLink({structure, otherStructure}) {
  describe("[Referential-NoReg] Create Communication Link Between Groups", () => {
    authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD)
    const rolesStruct1 = getRolesOfStructure(structure.id);
    const rolesStruct2 = getRolesOfStructure(otherStructure.id);
    const role1 = rolesStruct1[0];
    const role2 = rolesStruct2[0];
    let res = http.post(`${rootUrl}/communication/group/${role1.id}/communique/${role2.id}`, {}, { headers: getHeaders() });
    check(res, {
      "Creating communication link works": (r) => r.status === 200
    });
    // Now we try to delete the communication rule
    res = http.del(`${rootUrl}/communication/group/${role1.id}/communique/${role2.id}`, {}, { headers: getHeaders() });
    check(res, {
      "Deleting communication link works": (r) => r.status === 200
    });
  })
}

export function testSearchVisibles({structure}) {
  describe("[Referential-NoReg] SearchVisibles", () => {
    let session = authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD)
    const users = getUsersOfSchool(structure)
    const teacher = getRandomUserWithProfile(users, 'Teacher');
    session = authenticateWeb(teacher.login)
    
    const res = http.get(
        `${rootUrl}/communication/visible/search`,
        { headers: getHeaders() },
      );
      check(res, {
        "able to search visible entities": (r) => r.status === 200,
        "search visibles response is not empty": (r) => r !== null && JSON.parse(<string>res.body).length > 0,
        "response looks like an array of visibles": (r) => {
          const visibles = JSON.parse(<string>res.body);
          return Array.isArray(visibles) &&
           visibles.every(v => v.hasOwnProperty('id') &&
                               v.hasOwnProperty('displayName') &&
                               (v.hasOwnProperty('structureName') || v.hasOwnProperty('profile')) &&
                               v.hasOwnProperty('type'));
        }
      });
  })
}

export function testGroups({structure, otherStructure}) {
  describe("[Referential-NoReg] Groups", () => {
    authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD)
    const users = getUsersOfSchool(structure)
    const teacher = getRandomUserWithProfile(users, 'Teacher');
    let res = http.post(
      `${rootUrl}/directory/group`,
      JSON.stringify({name: `Groupe Manuel - ${Date.now()}`, structureId: structure.id}),
      { headers: getHeaders() },
    );
    check(res, {
      "able to create a group": (r) => r.status === 201,
      "group creation response is not empty": (r) => r !== null && JSON.parse(<string>res.body).id ,
      "response looks like a group": (r) => {
        const group = JSON.parse(<string>res.body);
        return group.hasOwnProperty('id') &&
                group.hasOwnProperty('createdAt') &&
                group.hasOwnProperty('createdByName') &&
                group.hasOwnProperty('modifiedAt') &&
                group.hasOwnProperty('modifiedByName');
      }
    });
    const groupId = JSON.parse(<string>res.body).id;

    // Change the group communication
    res = http.post(
      `${rootUrl}/communication/group/${groupId}?direction=BOTH`,
      JSON.stringify({}),
      {headers: getHeaders()});
    check(res, {
      "able to change group communication": (r) => r.status === 200,
      "change group communicationresponse is not empty": (r) => r !== null && JSON.parse(<string>res.body).number === 0
    });

    // Get group details
    res = http.get(`${rootUrl}/communication/group/${groupId}`, { headers: getHeaders() });
    check(res, {
      "able to get group details": (r) => r.status === 200,
      "get group details response is not empty": (r) => r !== null && JSON.parse(<string>res.body).id === groupId,
      "get group details response looks like a list of groups": (r) => {
        const group = JSON.parse(<string>res.body);
        return group.hasOwnProperty('id') &&
                group.hasOwnProperty('createdAt') &&
                group.hasOwnProperty('createdByName') &&
                group.hasOwnProperty('groupDisplayName') &&
                group.hasOwnProperty('name')
      }
  });

    // Add all users to the group
    res = http.put(`${rootUrl}/directory/group/${groupId}/users/add`,
    JSON.stringify({userIds: users.map(u => u.id)}), { headers: getHeaders() });
    check(res, {
      "able to add a user to the group": (r) => r.status === 200,
      "add user group response is empty": (r) => r.body === "{}"
    });

  // Get group composition
    res = http.get(`${rootUrl}/directory/user/admin/list?groupId=${groupId}`, { headers: getHeaders() });
    check(res, {
      "able to get group composition": (r) => r.status === 200,
      "get group composition response is not empty": (r) => r !== null && JSON.parse(<string>res.body).length >= 0,
      "get group composition response looks like an array of users": (r) => {
        const users = JSON.parse(<string>res.body);
        return Array.isArray(users) &&
               users.every(u => u.hasOwnProperty('id') &&
                               u.hasOwnProperty('displayName'))
      }})
  // Get the groups of the structure
    res = http.get(`${rootUrl}/directory/group/admin/list?structureId=${structure.id}`, { headers: getHeaders() });
    check(res, {
      "able to get groups of the structure": (r) => r.status === 200,
      "groups of structure response is not empty": (r) => r !== null && JSON.parse(<string>r.body).length >= 0,
      "groups of structure response elements look like an array of groups": (r) => {
        const groups = JSON.parse(<string>r.body);
        const isOk = Array.isArray(groups) &&
               groups.every(g => g.hasOwnProperty('id') &&
                                g.hasOwnProperty('name') &&
                                g.hasOwnProperty('nbUsers'));
        if(!isOk) {
          console.error("Groups of structure response is not an array of groups", groups);
        }
        return isOk;
      },
      "groups of structure response contains the created group": (r) => {
        const groups = JSON.parse(<string>res.body);
        return Array.isArray(groups) &&
               groups.some(g => g.id === groupId);
      }
    });

    // Add communication link with other groups
    const rolesStruct1 = getRolesOfStructure(otherStructure.id);
    for(const role1 of rolesStruct1) {
      if(role1.id !== groupId) {

        res = http.get(`${rootUrl}/communication/v2/group/${role1.id}/communique/${groupId}/check`, { headers: getHeaders() });
        check(res, {
          "able to check communication link between groups": (r) => r.status === 200
        });
        res = http.post(`${rootUrl}/communication/group/${role1.id}/communique/${groupId}`, {}, { headers: getHeaders() });
        res = http.post(`${rootUrl}/communication/v2/group/${groupId}/communique/${role1.id}`, {}, { headers: getHeaders() });
        check(res, {
          "able to create communication link between groups V2": (r) => r.status === 200
        });
        res = http.del(`${rootUrl}/communication/group/${groupId}/relations/${role1.id}`, {}, { headers: getHeaders() });
        check(res, {
          "able to remove links between groups": (r) => r.status === 200
        });
      }
    }

    const parent = getRandomUserWithProfile(users, 'Relative');
    res = http.get(`${rootUrl}/communication/verify/${teacher.id}/${parent.id}`, { headers: getHeaders() });
    check(res, {
      "able to verify communication link between teacher and parent": (r) => r.status === 200,
      "teacher can see parent": (r) => r !== null && JSON.parse(<string>res.body).canCommunicate === true
    });

    res = http.get(`${rootUrl}/communication/verify/${parent.id}/${teacher.id}`, { headers: getHeaders() });
    check(res, {
      "able to verify communication link between parent and teacher": (r) => r.status === 200,
      "parent cannot see teacher": (r) => r !== null && JSON.parse(<string>res.body).canCommunicate === false
    });

    // Check that we can get incoming and outoing communication links
    res = http.get(`${rootUrl}/communication/group/${groupId}/outgoing`, { headers: getHeaders() });
    check(res, {
      "able to get outgoing communication links of the group": (r) => r.status === 200,
      "outgoing communication links response is not empty": (r) => r !== null && JSON.parse(<string>res.body).length >= 0,
      "outgoing communication links response looks like an array of groups": (r) => {
        const groups = JSON.parse(<string>res.body);
        return Array.isArray(groups) &&
               groups.every(g => g.hasOwnProperty('id') &&
               g.hasOwnProperty('name') && g.hasOwnProperty('filter'));
      }
    });
    res = http.get(`${rootUrl}/communication/group/${groupId}/incoming`, { headers: getHeaders() });
    check(res, {
      "able to get incoming communication links of the group": (r) => r.status === 200,
      "incoming communication links response is not empty": (r) => r !== null && JSON.parse(<string>res.body).length >= 0,
      "incoming communication links response looks like an array of groups": (r) => {
        const groups = JSON.parse(<string>res.body);
        return Array.isArray(groups) &&
               groups.every(g => g.hasOwnProperty('id') &&
               g.hasOwnProperty('name') && g.hasOwnProperty('filter'));
      }
    });

    // Check that we can add link between relative and student
    res = http.post(`${rootUrl}/communication/relative/${groupId}?direction=BOTH`, '{}', { headers: getHeaders() });
    check(res, {
      "able to add link between relative and group": (r) => r.status === 200,
      "add link between relative and group response is not empty": (r) => r !== null && JSON.parse(<string>res.body).number > 0
    });

    // Check that we can remove link between relative and student
    res = http.del(`${rootUrl}/communication/relative/${groupId}?direction=BOTH`, {}, { headers: getHeaders() });
    check(res, {
      "able to remove link between relative and group": (r) => r.status === 200,
      "remove link between relative and group response is not empty": (r) => r !== null && JSON.parse(<string>res.body).number > 0
    });

    // Check that we can get a user's visible users
    res = http.get(`${rootUrl}/communication/visible/${teacher.id}`, { headers: getHeaders() });
    check(res, {
      "able to get a user's visibles": (r) => r.status === 200,
      "get a user's visibles response is not empty": (r) => r !== null && JSON.parse(<string>res.body).length >= users.length -1
    });

    authenticateWeb(teacher.login)
    // Check that we can get a user's visible users who are part of a group
    res = http.get(`${rootUrl}/communication/visible/group/${groupId}`, { headers: getHeaders() });
    check(res, {
      "able to get a user's visibles who are part of a group": (r) => r.status === 200,
      "get a user's visibles who are part of a group response is not empty": (r) => r !== null && JSON.parse(<string>res.body).length >= users.length -1,
      "get a user's visibles who are part of a group response looks like an array of users": (r) => {
        const visibles = JSON.parse(<string>res.body);
        return Array.isArray(visibles) &&
               visibles.every(v => v.hasOwnProperty('id') && v.hasOwnProperty('login'));
      }
    });

    authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD)

    // Remove link with users
    res = http.del(`${rootUrl}/directory/group/${groupId}?direction=BOTH`, '', { headers: getHeaders() });
    check(res, {
      "able to remove link with users": (r) => r.status === 204,
    });

    // Now we try to delete the group
    res = http.del(`${rootUrl}/directory/group/${groupId}`, {}, { headers: getHeaders() });
    check(res, {
      "able to delete a group": (r) => r.status === 204
    });
  })
}

export function testClassEndpoints({structure, otherStructure}) {
  // Test here
  // @Get("/class/:classId")
  // @Put("/class/:classId")
  // @Delete("/class/:classId")
  // @Post("/class/:classId/user")
  // @Get("/class/:classId/users")
  // @Put("/class/add-self")
  // @Put("/class/:classId/add/:userId")
  // @Put("/class/:classId/apply")
  // @Put("/class/:classId/link/:userId")
  // @Put("/class/:classId/link")
  // @Put("/class/:classId/unlink")
  // @Put("/class/:classId/change")
  // @Delete("/class/:classId/unlink/:userId")
  // @Get("/class/admin/list")
  // @Get("/class/users/detached")
  // @Get("/class/users/visibles")
  describe("[Referential-NoReg] Class", () => {
    check({},
      {"test on classes not implementeded yet": (r) => false}
    );
  })

}

export function testRules({structure, otherStructure}) {
  describe("[Referential-NoReg] Rules", () => {
    authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD)
    let res = http.put(
      `${rootUrl}/communication/init/rules`,
      JSON.stringify({ structures: [structure, otherStructure].map((s) => s.id) }),
      { headers:getHeaders() },
    );
    check(res, {
      "able to initialize rules": (r) => r.status === 200,
    });
    res = http.get(`${rootUrl}/communication/rules`, { headers: getHeaders() });
    check(res, {
      "able to get rules": (r) => r.status === 200,
      "rules response is not empty": (r) => r !== null && JSON.parse(<string>res.body).length > 0,
      "rules response looks like an array of rules": (r) => {
        const rules = JSON.parse(<string>res.body);
        return Object.keys(rules).every(key => {
          return key.indexOf('-') > 0 &&
                 rules[key].hasOwnProperty('users')
        });
      }
    });
    res = http.put(`${rootUrl}/communication/rules/${structure.id}`, {}, { headers: getHeaders() });
    check(res, {
      "able to update structure rules": (r) => r.status === 200,
      "update structure rules response is not empty": (r) => r !== null && JSON.parse(<string>res.body).status === "ok"
    });
    res = http.del(`${rootUrl}/communication/rules?structureId=${structure.id}`, {}, { headers: getHeaders() });
    check(res, {
      "able to delete structure rules": (r) => r.status === 200,
      "delete structure rules response is not empty": (r) => r !== null && JSON.parse(<string>res.body).status === "ok"
    });
  })
}

export function tesDiscover({structure, otherStructure}) {
  describe("[Referential-NoReg] Discoverability", () => {
    authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD)
    const users = getUsersOfSchool(structure)
    const teacher = getRandomUserWithProfile(users, 'Teacher');
    const teacher2 = getRandomUserWithProfile(users, 'Teacher', [teacher]);
    authenticateWeb(teacher.login)
    // Test discoverability endpoints
    let res = http.post(`${rootUrl}/communication/discover/visible/users`, {}, { headers: getHeaders() });
    check(res, {
      "able to discover visible users": (r) => r.status === 200,
      "discoverable users response is not empty": (r) => r !== null && JSON.parse(<string>res.body).length > 0,
      "discoverable users response looks like an array of users": (r) => {
        const visibleResponse = JSON.parse(<string>res.body);
        return Array.isArray(visibleResponse) &&
               visibleResponse.every(v => v.hasOwnProperty('id') &&
                                        v.hasOwnProperty('displayName') &&
                                        v.hasOwnProperty('groupDisplayName') &&
                                        v.hasOwnProperty('name') &&
                                        v.hasOwnProperty('profile') &&
                                        v.hasOwnProperty('hasCommunication') &&
                                        v.hasOwnProperty('structures'));
      }
    });

    res = http.get(`${rootUrl}/communication/discover/visible/profile`, { headers: getHeaders() });
    check(res, {
      "able to discover visible profiles": (r) => r.status === 200,
      "discoverable profiles response is not empty": (r) => r !== null && JSON.parse(<string>res.body).length > 0,
      "discoverable profiles response looks like an array of profiles": (r) => {
        const visibleResponse = JSON.parse(<string>res.body);
        return Array.isArray(visibleResponse) &&
               visibleResponse.every(v => typeof(v) === 'string' && v.length > 0);
      }
    });

    res = http.get(`${rootUrl}/communication/discover/visible/structures`, { headers: getHeaders() });
    check(res, {
      "able to discover visible structures": (r) => r.status === 200,
      "discoverable structures response is not empty": (r) => r !== null && JSON.parse(<string>res.body).length > 0,
      "discoverable structures response looks like an array of structures": (r) => {
        const visibleResponse = JSON.parse(<string>res.body);
        return Array.isArray(visibleResponse) &&
               visibleResponse.every(v => v.hasOwnProperty('id') &&
                                        v.hasOwnProperty('type') &&
                                        v.hasOwnProperty('label') &&
                                        v.hasOwnProperty('checked'));
      }
    });
    res = http.post(`${rootUrl}/communication/discover/visible/add/commuting/${teacher2.id}`, {}, { headers: getHeaders() });
    check(res, {
      "able to add a commuting user to visible": (r) => r.status === 200,
      "add commuting user response is not empty": (r) => r !== null && JSON.parse(<string>res.body).number === 1
    });
    res = http.del(`${rootUrl}/communication/discover/visible/remove/commuting/${teacher2.id}`, {}, { headers: getHeaders() });
    check(res, {
      "able to remove a commuting user from visible": (r) => r.status === 200,
      "remove commuting user response is not empty": (r) => r !== null && JSON.parse(<string>res.body).number === 1
    });


    res = http.get(`${rootUrl}/communication/discover/visible/groups`, { headers: getHeaders() });
    check(res, {
      "able to discover visible groups": (r) => r.status === 200,
      "discoverable groups response is not empty": (r) => r !== null && JSON.parse(<string>res.body).length > 0,
      "discoverable groups response looks like an array of groups": (r) => {
        const visibleResponse = JSON.parse(<string>res.body);
        return Array.isArray(visibleResponse) &&
               visibleResponse.every(v => v.hasOwnProperty('id') &&
                                        v.hasOwnProperty('name') &&
                                        v.hasOwnProperty('displayName') &&
                                       v.hasOwnProperty('nbUsers'));
      }
    });
    const groups = JSON.parse(<string>res.body);
    res = http.get(`${rootUrl}/communication/discover/visible/group/${groups[0].id}/users`, { headers: getHeaders() });
    check(res, {
      "able to discover visible users of a group": (r) => r.status === 200,
      "discoverable users of a group response is not empty": (r) => r !== null && JSON.parse(<string>res.body).length > 0,
      "discoverable users of a group response looks like an array of users": (r) => {
        const visibleResponse = JSON.parse(<string>res.body);
        return Array.isArray(visibleResponse) &&
              visibleResponse.every(v => v.hasOwnProperty('id') &&
                                        v.hasOwnProperty('type') &&
                                        v.hasOwnProperty('hasCommunication') &&
                                        v.hasOwnProperty('displayName') &&
                                        v.hasOwnProperty('login'));
      }
    });

    res = http.post(`${rootUrl}/communication/discover/visible/group`, JSON.stringify({name: `Groupe Manuel - ${Date.now()}`}), { headers: getHeaders() });
    check(res, {
      "able to create a discoverable group": (r) => r.status === 201,
      "create discoverable group response is not empty": (r) => r !== null && JSON.parse(<string>res.body).id,
    });
    const groupId = JSON.parse(<string>res.body).id;
    res = http.put(`${rootUrl}/communication/discover/visible/group/${groupId}`, JSON.stringify({name: `Groupe Manuel - Modified - ${Date.now()}`}), { headers: getHeaders() });
    check(res, {
      "able to modify a discoverable group": (r) => r.status === 200,
      "modify discoverable group response is not empty": (r) => r !== null && JSON.parse(<string>res.body).id === groupId,
    });

    res = http.put(`${rootUrl}/communication/discover/visible/group/${groupId}/users`, JSON.stringify({newUsers: [teacher2.id]}), { headers: getHeaders() });
    check(res, {
      "able to add users to a discoverable group": (r) => r.status === 200,
    });
    console.log("Added user to discoverable group", JSON.parse(<string>res.body));

});
}