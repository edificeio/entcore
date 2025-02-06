import { check, sleep } from "k6";
import {chai, describe } from "https://jslib.k6.io/k6chaijs/4.3.4.0/index.js";

import {
  authenticateWeb,
  getUsersOfSchool,
  createAndSetRole,
  linkRoleToUsers,
  triggerImport,
  initStructure,
  getRandomUserWithProfile,
  DraftMessage,
  SentMessage,
  sendMessage,
  createDraftMessage,
  getMessage,
} from "../../../node_modules/edifice-k6-commons/dist/index.js";

const aafImport = (__ENV.AAF_IMPORT || "true") === "true";
const aafImportPause =  parseInt(__ENV.AAF_IMPORT_PAUSE || "10");
const maxDuration = __ENV.MAX_DURATION || "1m";
const schoolName = __ENV.DATA_SCHOOL_NAME || "Conversation - Tests";
const dataRootPath = __ENV.DATA_ROOT_PATH;
const gracefulStop = parseInt(__ENV.GRACEFUL_STOP || "2s");

export const options = {
  setupTimeout: "1h",
  thresholds: {
    checks: ["rate == 1.00"],
  },
  scenarios: {
    conversationTest: {
      executor: "per-vu-iterations",
      vus: 1,
      maxDuration: maxDuration,
      gracefulStop,
    },
  },
};

let originalRichContent;
let transformedRichContent;
try {
  originalRichContent = open(`${dataRootPath}/conversation/original_rich_content.html`, 't');
  transformedRichContent = open(`${dataRootPath}/conversation/transformed_rich_content.html`, 't');
} catch(e) {
  originalRichContent = open(`${dataRootPath}/data/conversation/original_rich_content.html`, 't');
  transformedRichContent = open(`${dataRootPath}/data/conversation/transformed_rich_content.html`, 't');
}

export function setup() {
  let structure;
  describe("[Conversation-Init] Initialize data", () => {
    authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);
    structure = initSchool(schoolName)
  });
  if(aafImport) {
    triggerImport()
    sleep(aafImportPause)
  }
  return {structure}
}

function initSchool(structureName) {
  const structure = initStructure(structureName);
  const role = createAndSetRole('Messagerie');
  const groups = [
    `Teachers from group ${structure.name}.`,
    `Enseignants du groupe ${structure.name}.`,
  ]
  linkRoleToUsers(structure, role, groups);
  return structure;
}

export default (data) => {
  testTransformMessageContent(data)
  testTransformEmptyMessageContent(data)
}

function checkCreateOk(res, checkName) {
  const checks = {}
  checks[`${checkName} - HTTP status`] = (r) => r.status === 201
  const ok = check(res, checks);
  if(!ok) {
    console.error(checkName, res)
  }
}

function checkTransformOk(res, checkName) {
  const checks = {}
  checks[`${checkName} - HTTP status`] = (r) => r.status === 200
  checks[`${checkName} - Content has been transformed`] = (r) => {
    const sentMesage:SentMessage = JSON.parse(r.body)
    return !sentMesage.body.includes('div')
  }
  const ok = check(res, checks);
  if(!ok) {
    console.error(checkName, res)
  }
}

function checkMessageOk(res, senderId, recipientId, body, hasOriginal, checkName) {
  const checks = {}
  checks[`${checkName} - HTTP status`] = (r) => r.status === 200
  checks[`${checkName} - Content is transformed`] = (r) => {
    const message = JSON.parse(r.body)
    return !message.body.includes('div')
  }
  checks[`${checkName} - Content version is correct`] = (r) => {
    const message = JSON.parse(r.body)
    return message.content_version === 1
  }
  checks[`${checkName} - Sender is correct`] = (r) => {
    const message = JSON.parse(r.body)
    return message.from.id === senderId
  }
  if (recipientId.length !== 0) {
    checks[`${checkName} - Recipient is correct`] = (r) => {
      const message = JSON.parse(r.body)
      return message.to.users.map(item => item.id).includes(recipientId)
    }
  }
  checks[`${checkName} - Body is correct`] = (r) => {
    const message = JSON.parse(r.body)
    return message.body === body
  }
  checks[`${checkName} - Check original format`] = (r) => {
    const message = JSON.parse(r.body)
    return message.original_format_exists === hasOriginal
  }
  const ok = check(res, checks);
  if(!ok) {
    console.error(checkName, res)
  }
}

function checkMessageOriginalKo(res, messageId, checkName) {
  const checks = {}
  checks[`${checkName} - HTTP status`] = (r) => r.status === 400
  checks[`${checkName} - Original content not found`] = (r) => {
    const message = JSON.parse(r.body)
    return message.error === `No original content found for message with id : ${messageId}`
  }
  const ok = check(res, checks);
  if(!ok) {
    console.error(checkName, res)
  }
}

function testTransformMessageContent(data) {
  const {structure} = data;
  describe('[Conversation] Test - Create draft and send message', () => {
    authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);
    const users = getUsersOfSchool(structure);
    const teacher1 = getRandomUserWithProfile(users, 'Teacher')
    const teacher2 = getRandomUserWithProfile(users, 'Teacher', [teacher1])
    authenticateWeb(teacher1.login)
    const draftMessage = <DraftMessage>({
      to: [teacher2.id],
      cc: [],
      cci: [],
      subject: "message from teacher 1 to teacher 2",
      body: originalRichContent
    });
    // Teacher 1 creates draft message
    let res;
    res = createDraftMessage(draftMessage);
    checkCreateOk(res, 'Teacher 1 creates draft message')
    const messageId = JSON.parse(res.body).id;
    // Teacher 1 sends message to teacher 2
    res = sendMessage(messageId, draftMessage);
    checkTransformOk(res, 'Teacher 1 sends message to teacher 2')
    // Teacher 2 retrieves message
    authenticateWeb(teacher2.login)
    res = getMessage(messageId, false)
    checkMessageOk(res, teacher1.id, teacher2.id, transformedRichContent, false, 'Teacher 2 fetches message sent by teacher 1')
    // Teacher 2 fails fetching message original format (because it has been directely created with transformed content)
    res = getMessage(messageId, true)
    checkMessageOriginalKo(res, messageId, 'Teacher 2 fails to fetch original format of message')
  });
}

function testTransformEmptyMessageContent(data) {
  const {structure} = data;
  describe('[Conversation] Test - Transform message with empty body', () => {
    authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);
    const users = getUsersOfSchool(structure);
    const teacher = getRandomUserWithProfile(users, 'Teacher')
    authenticateWeb(teacher.login)
    const draftMessage = <DraftMessage>({
      to: [],
      cc: [],
      cci: [],
      subject: "draft message from teacher with empty body",
      body: ""
    });
    let res;
    // Teacher creates message with empty body
    res = createDraftMessage(draftMessage);
    checkCreateOk(res, 'Teacher creates draft message')
    const messageId = JSON.parse(res.body).id;
    // Teacher retrieves message with empty body
    res = getMessage(messageId, false)
    checkMessageOk(res, teacher.id, [], "", 'Teacher fetches message')
  });
}





