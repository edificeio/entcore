import {describe } from "https://jslib.k6.io/k6chaijs/4.3.4.0/index.js";

import {
  authenticateWeb,
  getUsersOfSchool,
  addCommRuleToGroup,
  attachStructureAsChild,
  initStructure,
  getRandomUserWithProfile,
  Session,
  searchVisibles,
  Structure,
  getProfileGroupOfStructureByType,
  UserProfileType,
  ShareBookMarkCreationRequest,
  createShareBookMarkOrFail,
  UserInfo
} from "../../../node_modules/edifice-k6-commons/dist/index.js";

const maxDuration = __ENV.MAX_DURATION || "5m";
const schoolName = __ENV.DATA_SCHOOL_NAME || "Maxi Users";
const gracefulStop = parseInt(__ENV.GRACEFUL_STOP || "2s");

export const options = {
  setupTimeout: "1h",
  thresholds: {
    checks: ["rate == 3.00"],
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
    const teacherProfileGroup = getProfileGroupOfStructureByType('Teacher', head)
    const attachedStructuresGroups: string[] = []
    for(let i=0; i < 10; i++) {
      const school = initStructure(`${schoolName} - School ${i}`);
      structures.push(school);
      attachStructureAsChild(head, school)
      for(let profile of profiles) {
        const schoolProfileGroup = getProfileGroupOfStructureByType(profile, school);
        attachedStructuresGroups.push(schoolProfileGroup.id);
      }
    }
    addCommRuleToGroup(teacherProfileGroup.id, attachedStructuresGroups);    
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
      name: 'test-share-bookmark-' + new Date().getDate(),
      members: members
    };
    //now call create bookmark that call get bookmark that check visible
    console.log("Authenticate heade teacher " + headTeacher.login);
    authenticateWeb(headTeacher.login);
    console.log("Creating and check share bookmark");
    const shareBookMark = createShareBookMarkOrFail(shareBookmarkRequest);
  });
};
