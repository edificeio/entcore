import {describe } from "https://jslib.k6.io/k6chaijs/4.3.4.0/index.js";

import {
  authenticateWeb,
  initStructure,
  Session,
  Structure,
  getProfileGroupsOfStructureByType,
  getProfileGroupsRelatedToGroup,
  resetRulesAndCheck,
  removeCommunicationBetweenGroups,
  getAdmlsOrMakThem
} from "../../../node_modules/edifice-k6-commons/dist/index.js";
import {ProfileGroup} from "edifice-k6-commons";
import {fail} from "k6";

const maxDuration = __ENV.MAX_DURATION || "5m";
const schoolName = __ENV.DATA_SCHOOL_NAME || "Test it admin";
const gracefulStop = parseInt(__ENV.GRACEFUL_STOP || "2s");

export const options = {
  setupTimeout: "1h",
  thresholds: {
    checks: ["rate == 1.00"],
  },
  scenarios: {
    testResetCommunicationsRules: {
      executor: "per-vu-iterations",
      exec: "testResetCommunicationsRules",
      vus: 1,
      maxDuration: maxDuration,
      gracefulStop,
    },
  },
};

type InitData = {
  structure: Structure;
}

export function setup() {
  let structure: Structure;
  describe("[Reset-Communications-Rules] Initialize data", () => {
    <Session>authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);
    structure = initStructure(`${schoolName} - School`);
  });
  return { structure };
}

export function testResetCommunicationsRules(data: InitData){
  
  describe('[Admin][Structure][Communication] Test that we can apply default communication rules ', () => {

    <Session>authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);

    const profileGroups: ProfileGroup[] = getProfileGroupsOfStructureByType("Teacher", data.structure);
    const profileGroupTeacherStruct: ProfileGroup = profileGroups.find(p => p.name === (schoolName + ' - School-Teacher'));

    if(profileGroupTeacherStruct === null) {
      fail("[Admin][Structure][Communication] Unable to find the teacher group of the structure");
    }
    let incomingRelation: ProfileGroup[] = getProfileGroupsRelatedToGroup(profileGroupTeacherStruct.id, "incoming");
    let outgoingRelation: ProfileGroup[] = getProfileGroupsRelatedToGroup(profileGroupTeacherStruct.id, "outgoing");

    for(let i = 0; i < incomingRelation.length; i++) {
      removeCommunicationBetweenGroups(incomingRelation[i].id, profileGroupTeacherStruct.id);
    }
    for(let i = 0; i < outgoingRelation.length; i++) {
      removeCommunicationBetweenGroups(profileGroupTeacherStruct.id, outgoingRelation[i].id);
    }

    incomingRelation = getProfileGroupsRelatedToGroup(profileGroupTeacherStruct.id, "incoming");
    outgoingRelation = getProfileGroupsRelatedToGroup(profileGroupTeacherStruct.id, "outgoing");

    if (incomingRelation !== null && incomingRelation.length > 0) {
      fail("[Admin][Structure][Communication] Incoming group communication should be empty");
    }
    if (outgoingRelation !== null && outgoingRelation.length > 0) {
      fail("[Admin][Structure][Communication] Outgoing group communication should be empty");
    }
    //reset all rules => the structure has no communication on the teacher group
    resetRulesAndCheck(data.structure, 200);

    const incomingUpdatedRelation: ProfileGroup[] = getProfileGroupsRelatedToGroup(profileGroupTeacherStruct.id, "incoming");
    const outgoingUpdatedRelation: ProfileGroup[] = getProfileGroupsRelatedToGroup(profileGroupTeacherStruct.id, "outgoing");

    if (incomingUpdatedRelation === null || incomingUpdatedRelation.length === 0) {
      fail("[Admin][Structure][Communication] Incoming group communication should not be empty");
    }
    if (outgoingUpdatedRelation === null || outgoingUpdatedRelation.length === 0) {
      fail("[Admin][Structure][Communication] Outgoing group communication should not be empty");
    }
  });

  describe('[Admin][Structure][Communication] Test that adml cant reset communications rules ', () => {

    <Session>authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);

    const admlTeacher = getAdmlsOrMakThem(data.structure, 'Teacher', 1, [])[0]
    authenticateWeb(admlTeacher.login)

    //reset all rules => the structure has no communication on the teacher group
    resetRulesAndCheck(data.structure, 401);
  });
}
