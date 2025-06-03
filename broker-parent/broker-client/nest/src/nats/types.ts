export interface ListenOnlyDTO {
  timestamp?: number;
  userId?: string;
}

export interface FetchTranslationsRequestDTO {
  application?: string;
  headers?: { [key: string]: string };
  langAndDomain?: LangAndDomain;
}

export interface UpdateGroupRequestDTO {
  externalId?: string;
  id?: string;
  name?: string;
}

export interface RemoveGroupMemberResponseDTO {
  removed?: boolean;
}

export interface GetUserDisplayNamesResponseDTO {
  userDisplayNames?: { [key: string]: string };
}

export interface AppRegistrationDTO {
  address?: string;
  appType?: string;
  customProperties?: { [key: string]: object };
  display?: boolean;
  displayName?: string;
  icon?: string;
  name?: string;
  prefix?: string;
}

export interface ListenAndAnswerDTO {
  jobId?: string;
  userId?: string;
}

export interface UpsertGroupSharesRequestDTO {
  application?: string;
  currentUserId?: string;
  groupId?: string;
  permissions?: string[];
  resourceId?: string;
}

export interface RemoveGroupSharesResponseDTO {
  shares?: SharesResponseDTO[];
}

export interface AppRegistrationRequestDTO {
  actions?: SecuredActionDTO[];
  application?: AppRegistrationDTO;
}

export interface FindGroupByExternalIdRequestDTO {
  externalId?: string;
}

export interface FindSessionResponseDTO {
  session?: SessionDto;
}

export interface FetchTranslationsResponseDTO {
  translations?: { [key: string]: string };
}

export interface RemoveGroupSharesRequestDTO {
  application?: string;
  currentUserId?: string;
  groupExternalId?: string;
  groupId?: string;
  resourceId?: string;
}

export interface AddGroupMemberResponseDTO {
  added?: boolean;
}

export interface DummyResponseDTO {
  jobId?: string;
  success?: boolean;
  userId?: string;
}

export interface RegisterTranslationFilesResponseDTO {
  application?: string;
  languagesCount?: number;
  translationsCount?: number;
}

export interface UpsertGroupSharesResponseDTO {
  shares?: SharesResponseDTO[];
}

export interface CreateGroupRequestDTO {
  classId?: string;
  externalId?: string;
  name?: string;
  structureId?: string;
}

export interface CreateGroupResponseDTO {
  id?: string;
}

export interface UpdateGroupResponseDTO {
  updated?: boolean;
}

export interface RemoveGroupMemberRequestDTO {
  groupExternalId?: string;
  groupId?: string;
  userId?: string;
}

export interface FindSessionRequestDTO {
  cookies?: string;
  headers?: { [key: string]: string };
  params?: { [key: string]: string };
  path?: string;
  pathPrefix?: string;
  sessionId?: string;
}

export interface RegisterTranslationFilesRequestDTO {
  application?: string;
  translationsByLanguage?: { [key: string]: { [key: string]: string } };
}

export interface DeleteGroupResponseDTO {
  deleted?: boolean;
}

export interface GroupDTO {
  id?: string;
  name?: string;
}

export interface SessionDto {
  authorizedActions?: ActionDto[];
  birthDate?: string;
  classes?: ClassDto[];
  email?: string;
  externalId?: string;
  firstName?: string;
  functions?: UserFunctionDto[];
  groups?: GroupDto[];
  lastName?: string;
  level?: string;
  login?: string;
  mobile?: string;
  structures?: StructureDto[];
  superAdmin?: boolean;
  type?: string;
  userId?: string;
  username?: string;
}

export interface AppRegistrationResponseDTO {
  message?: string;
  success?: boolean;
}

export interface DeleteGroupRequestDTO {
  externalId?: string;
  id?: string;
}

export interface FindGroupByExternalIdResponseDTO {
  group?: GroupDTO;
}

export interface LangAndDomain {
  domain?: string;
  lang?: string;
}

export interface AddGroupMemberRequestDTO {
  groupExternalId?: string;
  groupId?: string;
  userId?: string;
}

export interface GetUserDisplayNamesRequestDTO {
  userIds?: string[];
}