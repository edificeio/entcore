import { RefinedResponse } from "k6/http";
import {
  authenticateWeb,
  Structure,
  getUsersOfSchool,
  getHeaders,
  UserProfileType,
  UserInfo,
  getSchoolByName,
  createEmptyStructure,
  assertOk,
  createUser
} from "../../../node_modules/edifice-k6-commons/dist/index.js";
import http from "k6/http";
import {check, group, sleep} from "k6";


const maxDuration = __ENV.MAX_DURATION || "20m";
const schoolName = __ENV.DATA_SCHOOL_NAME || "Directory";
const schoolNameCSV = __ENV.DATA_SCHOOL_NAME_CSV || `${schoolName} CSV`;
const gracefulStop = parseInt(__ENV.GRACEFUL_STOP || "2s");
const rootUrl = __ENV.ROOT_URL;
const skipInit = __ENV.SKIP_INIT === "true";
const indexStart = parseInt(__ENV.INDEX_START || "9999");
const MAX_WAIT_STRUCTURE_IMPORTED = parseInt(__ENV.MAX_WAIT_STRUCTURE_IMPORTED || "30");
const nbDuplicateUsers = parseInt(__ENV.NB_DUPLICATE_USERS || "3");

const types: UserProfileType[] = ['Teacher', 'Relative', 'Student'];

export const options = {
  setupTimeout: "1h",
  thresholds: {
    checks: ["rate == 1.00"],
  },
  scenarios: {
    testImportAAF: {
      executor: "per-vu-iterations",
      exec: "testImportAAF",
      vus: 1,
      maxDuration: maxDuration,
      gracefulStop,
    },
  },
};

type InitData = {
  aaf: {
    structure: Structure;
    users: UserInfo[];
  };
  csv: {
    structure: Structure;
    users: UserInfo[];
  };
}

type AAFStructureGenerationParameters = {
  structureName: string;
  nbStudents: number;
  nbTeachers: number;
  nbRelatives: number;
}

export function setup() {
  const initData: InitData = {
  }
  let structureAAF: Structure;
  let structureCSV: Structure;
  let users: UserInfo[];

  group("[Directory] Initialize data", () => {
    authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);
    structureCSV = createEmptyStructure(schoolNameCSV, true);
    let usersCSV = getUsersOfSchool(structureCSV);
    if(usersCSV.length < nbDuplicateUsers) {
      for( let i = usersCSV.length; i < nbDuplicateUsers; i++) {
        createUser({
          firstName: `First Name Teach ${i}`,
          lastName: `LAST NAME TEACH ${i}`,
          type: 'Teacher',
          structureId: structureCSV.id,
          birthDate: "01/01/1986",
          positionIds: []});
      }
      usersCSV = getUsersOfSchool(structureCSV);
    }
    initData.csv = { structure: structureCSV, users: usersCSV };


    structureAAF = generateAAFStructure([{ structureName: schoolName, nbStudents: 10, nbTeachers: 5, nbRelatives: 3 }]);
    users = getUsersOfSchool(structureAAF);
    initData.aaf = { structure: structureAAF, users };


  });
  console.log("[Directory] Setup completed");
  return initData;
}

/*******************************************************************************************************
 *  User Endpoints
 ******************************************************************************************************/
export function testImportAAF(data: InitData) {
  authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);
  const idsOfDuplicatedUsersInCSV = data.csv.users.filter(u => u.duplicates && u.duplicates.length > 0).map((u) => u.id);
  const idsOfDuplicatedUsersInAAF = data.aaf.users.filter(u => u.duplicates && u.duplicates.length > 0).map((u) => u.id);
  let res = http.get(`${rootUrl}/directory/duplicates?structure=${data.aaf.structure.id}`, { headers: getHeaders() });
  check(res, {
    'list duplicates with structure filter returns 200': (r) => r.status === 200,
    'list duplicates with structure filter is array': (r) => Array.isArray(JSON.parse(<string>r.body)),
    'list duplicates with structure filter has expected length': (r) => JSON.parse(<string>r.body).length === nbDuplicateUsers,
    'list duplicates with structure filter has expected content': (r) => {
      const duplicates = JSON.parse(<string>r.body);
      return duplicates.every((d: any) => {
        const userId1 = d.user1.id;
        const userId2 = d.user2.id;
        return idsOfDuplicatedUsersInCSV.includes(userId1) && idsOfDuplicatedUsersInAAF.includes(userId2) ||
               idsOfDuplicatedUsersInCSV.includes(userId2) && idsOfDuplicatedUsersInAAF.includes(userId1);
      });
    }
  });

  res = http.get(`${rootUrl}/directory/duplicates`, { headers: getHeaders() });
  const duplicates = new Set();
  JSON.parse(<string>res.body).forEach((d: any) => {
    const userId1 = d.user1.id;
    const userId2 = d.user2.id;
    duplicates.add(userId1);
    duplicates.add(userId2);
  });
  check(res, {
    'list duplicates without structure filter returns 200': (r) => r.status === 200,
    'list duplicates without structure filter is array': (r) => Array.isArray(JSON.parse(<string>r.body)),
    'list duplicates without structure filter has expected length': (r) => JSON.parse(<string>r.body).length >= nbDuplicateUsers,
    'list duplicates without structure filter has all duplicated of CSV structure': (r) => {
      return idsOfDuplicatedUsersInCSV.every((id) => duplicates.has(id));
    },
    'list duplicates without structure filter has all duplicated of AAF structure': (r) => {
      return idsOfDuplicatedUsersInAAF.every((id) => duplicates.has(id));
    }
  });
}

function generateAAFStructure(parameters: AAFStructureGenerationParameters[]): Structure {
  const request = generateAAFStructureRequest(parameters);
  importAAFStructureOrFail(request);
  const res = triggerAAFImport();
  assertOk(res, "AAF import should be triggered", 202);
  let structure: Structure | null = null;
  let endWait = Date.now() + MAX_WAIT_STRUCTURE_IMPORTED * 1000;
  while(Date.now() < endWait) {
    structure = getSchoolByName(schoolName);
    if(structure) {
      console.log(`Structure ${schoolName} found after ${MAX_WAIT_STRUCTURE_IMPORTED - Math.floor((endWait - Date.now()) / 1000)} seconds`);
      break;
    }
    console.log(`Waiting for structure ${schoolName} to be imported...`);
    sleep(1);
  }
  if(!structure) {
    throw new Error(`Structure ${schoolName} not found after ${MAX_WAIT_STRUCTURE_IMPORTED} seconds`);
  }
  while(Date.now() < endWait) {
    const expectedNbUsers = parameters[0].nbStudents + parameters[0].nbTeachers + parameters[0].nbStudents * 2;
    const users = getUsersOfSchool(structure);
    if(users.length >= expectedNbUsers) {
      console.log(`All users found after ${MAX_WAIT_STRUCTURE_IMPORTED - Math.floor((endWait - Date.now()) / 1000)} seconds`);
      break;
    }
    console.log(`Waiting for all users to be imported (${users.length} out of ${expectedNbUsers})...`);
    sleep(1);
  }
  return structure;
}

function triggerAAFImport(): RefinedResponse<any> {
  const headers = getHeaders("application/json");
  const triggerRequest = {
    feeder: "AAF"
  };
  const res = http.post(
    `${rootUrl}/directory/api/internal/trigger-import`,
    JSON.stringify(triggerRequest),
    { headers },
  );
  return res;
}

function generateAAFStructureRequest(parameters: AAFStructureGenerationParameters[]): Record<string, string> {
  let eleves = "";
  let teachers = "";
  let relatives = "";
  const structures = parameters.map((param, index) => {
    const structureId = `STRUCTURE_${index + indexStart}`;
    const structParam = {
      ID: structureId,
      UAI: "",
      NAME: param.structureName,
    }
    let structureXml = replaceParams(structureTemplate, structParam);
    for(let i = 0; i < param.nbStudents; i++) {
      // Generate 2 parents for each student
      let parentIdx = 0;
      let fatherIdx = 0;
      let motherIdx = 0;
      for(let j = 0; j < 2; j++) {
        parentIdx = indexStart + (index * parameters.length) + (i * param.nbStudents) + (j + 1);
        const father = j%2 === 0;
        if(father) {
          fatherIdx = parentIdx;
        } else {
          motherIdx = parentIdx;
        }
        const relativeParam = {
          ID: parentIdx.toString(),
          LASTNAME: `Last Name Rel ${i}_${j}`,
          FIRSTNAME: `First Name Rel ${i}_${j}`,
          TITLE: j%2 === 0 ? "M" : "F",
          BIRTHDATE: "01/01/1980",
        }
        relatives += replaceParams(persRelEleveTemplate, relativeParam);
      }
      const studentParam = {
        ID: String(indexStart + index * parameters.length + i * param.nbStudents),
        STRUCTURE_ID: structureId,
        LASTNAME: `Last Name Stud ${i}`,
        FIRSTNAME: `First Name Stud ${i}`,
        TITLE: i%2 === 0 ? "M" : "F",
        PARENTS_ID: parentIdx.toString(),
        FATHER_ID: fatherIdx.toString(),
        MOTHER_ID: motherIdx.toString(),
        PARENTAL_AUTHORITY_ID: Math.random() < 0.5 ? fatherIdx.toString() : motherIdx.toString(),
        BIRTHDATE: "01/01/2020",
      }
      eleves += replaceParams(persEleveTemplate, studentParam);
    }
    for (let i = 0; i < param.nbTeachers; i++) {
      const teacherParam = {
        ID: String(indexStart + index * parameters.length + (param.nbStudents * 3) + i),
        STRUCTURE_ID: structureId,
        LASTNAME: `Last Name Teach ${i}`,
        FIRSTNAME: `First Name Teach ${i}`,
        TITLE: i%2 === 0 ? "M" : "F",
        BIRTHDATE: "01/01/1970",
      }
      teachers += replaceParams(persEducNatTemplate, teacherParam);
    }
    return structureXml;
  }).join("\n");
  return {
    "ENT_IT_Complet_EtabEducNat_0001.xml": insertValuesIntoAAfTemplate(structures),
    "ENT_IT_Complet_Eleve_0001.xml": insertValuesIntoAAfTemplate(eleves),
    "ENT_IT_Complet_PersEducNat_0001.xml": insertValuesIntoAAfTemplate(teachers),
    "ENT_IT_Complet_PersRelEleve_0001.xml": insertValuesIntoAAfTemplate(relatives),
    "ficAlimMENESR.dtd": ficAlimMENESRDTD
  } 
}

function insertValuesIntoAAfTemplate(values: string): string {
  return aafFileTemplate.replace("###DATA###", values);
}

function replaceParams(template: string, params: Record<string, string>): string {
  let result = template;
  for (const [key, value] of Object.entries(params)) {
    const placeholder = `###${key}###`;
    result = result.replace(new RegExp(placeholder, "g"), value);
  }
  return result;
}

export function importAAFStructure(files: Record<string, string>): RefinedResponse<any> {
  const request: AAFFilesUploadRequest = {
    subPath: "test",
    files: files,
  };
  const headers = getHeaders("application/json");
  const res = http.post(
    `${rootUrl}/directory/api/internal/upload-aaf`,
    JSON.stringify(request),
    { headers },
  );
  return res;
}

export function importAAFStructureOrFail(files: Record<string, string>) {
  const res = importAAFStructure(files);
  assertOk(res, "AAF structure should be imported");
}

export type AAFFilesUploadRequest = {
  subPath: string;
  files: Record<string, string>;
}

const structureTemplate = `<addRequest>
<operationalAttributes><attr name="categorieStructure"><value>EtabEducNat</value></attr></operationalAttributes> 
<identifier><id>###ID###</id></identifier> 
<attributes> 
<attr name="ENTStructureJointure"><value>###ID###</value></attr>
<attr name="ENTStructureUAI"><value>###UAI###</value></attr>
<attr name="ENTEtablissementUAI"><value>###UAI###</value></attr>
<attr name="ENTStructureSIREN"><value/></attr>
<attr name="ENTStructureNomCourant"><value>###NAME###</value></attr>
<attr name="ENTStructureTypeStruct"><value/></attr>
<attr name="ENTEtablissementMinistereTutelle"><value>MINISTERE DE L&apos;EDUCATION NATIONALE</value></attr>
<attr name="ENTEtablissementContrat"><value>PU</value></attr>
<attr name="postOfficeBox"><value></value></attr>
<attr name="street"><value></value></attr>
<attr name="postalCode"><value></value></attr>
<attr name="l"><value></value></attr>
<attr name="telephoneNumber"><value></value></attr>
<attr name="facsimileTelephoneNumber"><value></value></attr>
<attr name="ENTEtablissementStructRattachFctl"><value/></attr>
<attr name="ENTEtablissementBassin"><value></value></attr>
<attr name="ENTServAcAcademie"><value>TEST</value></attr>
<attr name="ENTStructureClasses"><value></value></attr>
<attr name="ENTStructureGroupes"><value></value></attr>
</attributes>
</addRequest>`;

const persRelEleveTemplate = `<addRequest>
<operationalAttributes><attr name="categoriePersonne"><value>PersRelEleve</value></attr></operationalAttributes>
<identifier><id>###ID###</id></identifier>
<attributes>
<attr name="ENTPersonJointure"><value>###ID###</value></attr>
<attr name="ENTPersonDateNaissance"><value>###BIRTHDATE###</value></attr>
<attr name="ENTPersonNomPatro"><value>###LASTNAME###</value></attr>
<attr name="sn"><value>###LASTNAME###</value></attr>
<attr name="givenName"><value>###FIRSTNAME###</value></attr>
<attr name="personalTitle"><value>###TITLE###</value></attr>
<attr name="homePhone"><value/></attr>
<attr name="telephoneNumber"><value/></attr>
<attr name="ENTPersonAdresse"><value></value></attr>
<attr name="ENTPersonCodePostal"><value></value></attr>
<attr name="ENTPersonVille"><value></value></attr>
<attr name="ENTPersonPays"><value>FRANCE</value></attr>
<attr name="ENTPersonAdresseDiffusion"><value>N</value></attr>
<attr name="mobile"><value></value></attr>
<attr name="mail"><value/></attr>
<attr name="ENTPersonMobileSMS"><value/></attr>
</attributes>
</addRequest>`;
const persEducNatTemplate = `<addRequest>
<operationalAttributes><attr name="categoriePersonne"><value>PersEducNat</value></attr></operationalAttributes>
<identifier><id>###ID###</id></identifier>
<attributes>
<attr name="ENTPersonJointure"><value>###ID###</value></attr>
<attr name="ENTPersonDateNaissance"><value>###BIRTHDATE###</value></attr>
<attr name="ENTPersonNomPatro"><value>###LASTNAME###</value></attr>
<attr name="sn"><value>###LASTNAME###</value></attr>
<attr name="givenName"><value>###FIRSTNAME###</value></attr>
<attr name="personalTitle"><value>###TITLE###</value></attr>
<attr name="mail"><value></value></attr>
<attr name="ENTPersonStructRattach"><value>###STRUCTURE_ID###</value></attr>
<attr name="ENTAuxEnsCategoDiscipline"><value></value></attr>
<attr name="ENTAuxEnsDisciplinesPoste"><value></value></attr>
<attr name="ENTAuxEnsMEF"><value/></attr>
<attr name="ENTAuxEnsMatiereEnseignEtab"><value/></attr>
<attr name="ENTAuxEnsClasses"><value/></attr>
<attr name="ENTAuxEnsGroupes"><value/></attr>
<attr name="ENTAuxEnsClassesMatieres"><value>###STRUCTURE_ID###$1TES 2</value></attr>
<attr name="ENTAuxEnsGroupesMatieres"><value/></attr>
<attr name="ENTAuxEnsClassesPrincipal"><value/></attr>
<attr name="ENTPersonFonctions"><value>###STRUCTURE_ID###$ENS$ENSEIGNEMENT$P0210$LETTRES HISTOIRE GEOGRAPHIE</value></attr>
<attr name="PersEducNatPresenceDevantEleves"><value>O</value></attr>
</attributes>
</addRequest>`;
const persEleveTemplate = `<addRequest>
<operationalAttributes><attr name="categoriePersonne"><value>Eleve</value></attr></operationalAttributes>
<identifier><id>###ID###</id></identifier>
<attributes>
<attr name="ENTPersonJointure"><value>###ID###</value></attr>
<attr name="ENTEleveStructRattachId"><value>###STRUCTURE_ID###</value></attr>
<attr name="ENTPersonDateNaissance"><value>###BIRTHDATE###</value></attr>
<attr name="ENTPersonNomPatro"><value>###LASTNAME###</value></attr>
<attr name="sn"><value>###LASTNAME###</value></attr>
<attr name="givenName"><value>###FIRSTNAME###</value></attr>
<attr name="ENTPersonAutresPrenoms"><value>###FIRSTNAME###</value></attr>
<attr name="personalTitle"><value>###TITLE###</value></attr>
<attr name="ENTEleveParents"><value>###PARENTS_ID###</value></attr>
<attr name="ENTElevePere"><value>###FATHER_ID###</value></attr>
<attr name="ENTEleveMere"><value>###MOTHER_ID###</value></attr>
<attr name="ENTEleveAutoriteParentale"><value>###PARENTAL_AUTHORITY_ID###</value></attr>
<attr name="ENTElevePersRelEleve1"><value/></attr>
<attr name="ENTEleveQualitePersRelEleve1"><value></value></attr>
<attr name="ENTElevePersRelEleve2"><value/></attr>
<attr name="ENTEleveQualitePersRelEleve2"><value/></attr>
<attr name="ENTElevePersRelEleve"><value>###FATHER_ID###$1$0$1$0$0</value><value>###MOTHER_ID###$1$0$1$0$0</value></attr>
<attr name="ENTEleveBoursier"><value>N</value></attr>
<attr name="ENTEleveRegime"><value>EXTERNE LIBRE</value></attr>
<attr name="ENTEleveTransport"><value>N</value></attr>
<attr name="ENTEleveStatutEleve"><value>SCOLAIRE</value></attr>
<attr name="ENTEleveMEF"><value></value></attr>
<attr name="ENTEleveLibelleMEF"><value>PREMIERE ECONOMIQUE ET SOCIALE</value></attr>
<attr name="ENTEleveNivFormation"><value>PREMIERE GENERALE &amp; TECHNO YC BT</value></attr>
<attr name="ENTEleveFiliere"><value>1ERE  GENERALE</value></attr>
<attr name="ENTEleveEnseignements"><value>ACCOMPAGNEMENT PERSONNALISE</value><value>ANGLAIS LV1</value><value>EDUCATION PHYSIQUE ET SPORTIVE</value><value>ESPAGNOL LV2</value><value>FRANCAIS</value><value>MATHEMATIQUES</value><value>SCIENCES</value><value>SCIENCES ECONOMIQUES ET SOCIALES</value><value>TRAVAUX PERSONNELS ENCADRES</value><value>VIE DE CLASSE</value><value>HISTOIRE-GEOGRAPHIE</value><value>ENSEIGNEMENT MORAL ET CIVIQUE</value></attr>
<attr name="ENTEleveCodeEnseignements"><value></value></attr>
<attr name="ENTPersonStructRattach"><value>###STRUCTURE_ID###</value></attr>
<attr name="ENTEleveClasses"><value>###STRUCTURE_ID###$1TES 2</value></attr>
<attr name="ENTEleveGroupes"><value/></attr>
</attributes>
</addRequest>
`;

const aafFileTemplate = `<?xml version="1.0" encoding="ISO-8859-15"?>
<!DOCTYPE ficAlimMENESR SYSTEM "ficAlimMENESR.dtd">
<ficAlimMENESR>
###DATA###
</ficAlimMENESR>`;


const ficAlimMENESRDTD = `<!ELEMENT ficAlimMENESR (addRequest|modifyRequest|deleteRequest)*>
<!ELEMENT addRequest (operationalAttributes, identifier, attributes)>
<!ELEMENT modifyRequest (operationalAttributes, identifier, modifications)>
<!ELEMENT deleteRequest (operationalAttributes, identifier)>
<!ELEMENT operationalAttributes (attr)> <!-- Pas de controle : l'attribut "name" de l'element "attr" doit etre egal a "categoriePersonne" ou "categorieStructure" -->
<!ELEMENT identifier (id)>
<!ELEMENT attributes (attr+)>
<!ELEMENT attr (value+)>
<!ELEMENT modifications (modification+)>
<!ELEMENT modification (value+)>
<!ELEMENT value (#PCDATA)>
<!ELEMENT id (#PCDATA)>
<!ATTLIST attr name CDATA #REQUIRED>
<!ATTLIST modification
	name CDATA #REQUIRED
	operation (replace) #REQUIRED>`;