import {describe } from "https://jslib.k6.io/k6chaijs/4.3.4.0/index.js";

import {
  authenticateWeb,
  getUsersOfSchool,
  addCommRuleToGroup,
  attachStructureAsChild,
  initStructure,
  getRandomUserWithProfile,
  Session,
  Structure,
  getTeacherRole,
  getRolesOfStructure,
  UserProfileType,
  ShareBookMarkCreationRequest,
  createShareBookMarkOrFail
} from "../../../node_modules/edifice-k6-commons/dist/index.js";
import {
  checkNbBookmarkVisible
} from "./_sharebookmark-utils.ts";

const maxDuration = __ENV.MAX_DURATION || "5m";
const schoolName = __ENV.DATA_SCHOOL_NAME || "Maxi Users";
const gracefulStop = parseInt(__ENV.GRACEFUL_STOP || "2s");

export const options = {
  setupTimeout: "1h",
  thresholds: {
    checks: ["rate == 1.00"],
  },
  scenarios: {
    testCanSeeUsersFromBookMark: {
      executor: "per-vu-iterations",
      exec: "testCanSeeUsersFromBookMark",
      vus: 1,
      maxDuration: maxDuration,
      gracefulStop,
    },
  },
};

type InitData = {
  head: Structure;
  structures: Structure[];
}

export function setup() {
  let head :Structure|null = null;
  const structures: Structure[] = [];
  const profiles: UserProfileType[] = ['Teacher', 'Student', 'Relative'];
  describe("[Bookmark-Init] Initialize data", () => {
    <Session>authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);
    head = initStructure(`${schoolName} - Head`)
    const teacherProfileGroup = getTeacherRole(head);
    const attachedStructuresGroups: string[] = []
    for(let i=0; i < 10; i++) {
      const school = initStructure(`${schoolName} - School ${i}`);
      structures.push(school);
      attachStructureAsChild(head, school)
      const schoolProfileGroup = getRolesOfStructure(school.id);
      attachedStructuresGroups.push(...schoolProfileGroup.map((s) => s.id));
    }
    for(let group of attachedStructuresGroups) {
      addCommRuleToGroup(group, [teacherProfileGroup.id]);
    }
  });
  return { head, structures};
}

export function testCanSeeUsersFromBookMark(data: InitData){
  
  describe('[Bookmark] Test that a user can see favourite users ', () => {

    <Session>authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);
    const headUsers = getUsersOfSchool(data.head);
    const headTeacher = getRandomUserWithProfile(headUsers, 'Teacher');

    // Create a share bookmark with the users of all the structures
    const members: string[] = [];
    console.log("Adding user on head structure to bookmark");
    const users = getUsersOfSchool(data.head).map((u) => u.id);
    members.push(...users);      

    const shareBookmarkRequest: ShareBookMarkCreationRequest = {
      name: 'test-share-bookmark-' + new Date().getTime(),
      members: members
    };
    //now call create bookmark that call get bookmark that check visible
    console.log("Authenticate head teacher " + headTeacher.login);
    authenticateWeb(headTeacher.login);
    console.log("Creating and check share bookmark");
    const bookmark = createShareBookMarkOrFail(shareBookmarkRequest);
    checkNbBookmarkVisible(bookmark, members.length - 1, "Head bookmark");

    //try to create a bookmark on all the hierarchy
    <Session>authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);

    for(let structure of data.structures) {
      const schoolUsers = getUsersOfSchool(structure).map((u) => u.id);
      members.push(...schoolUsers);
    }

    const shareBookMarkAllStruct: ShareBookMarkCreationRequest = {
      name: 'test-share-bookmark-all-' + new Date().getTime(),
      members: members
    };

     //now call create bookmark that call get bookmark that check visible
    console.log("Authenticate head teacher " + headTeacher.login);
    authenticateWeb(headTeacher.login);
    console.log("Creating and check share bookmark on all struct");
    const allStructBookmark = createShareBookMarkOrFail(shareBookMarkAllStruct);

    checkNbBookmarkVisible(allStructBookmark, members.length - 1, "All struct bookmark");
  });
};
