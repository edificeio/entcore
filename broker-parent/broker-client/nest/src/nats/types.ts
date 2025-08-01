export interface ActionDto {
  displayName?: string;
  name?: string;
  type?: string;
}

export interface AddCommunicationLinksRequestDTO {
  direction?: string;
  groupId?: string;
}

export interface AddCommunicationLinksResponseDTO {
  added?: boolean;
}

export interface AddGroupMemberRequestDTO {
  groupExternalId?: string;
  groupId?: string;
  userId?: string;
}

export interface AddGroupMemberResponseDTO {
  added?: boolean;
}

export interface AddLinkBetweenGroupsRequestDTO {
  endGroupId?: string;
  startGroupId?: string;
}

export interface AddLinkBetweenGroupsResponseDTO {
  created?: boolean;
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

export interface AppRegistrationRequestDTO {
  actions?: SecuredActionDTO[];
  application?: AppRegistrationDTO;
}

export interface AppRegistrationResponseDTO {
  message?: string;
  success?: boolean;
}

export interface BaseCalendar {
  ACCUMULATED_DAYS_IN_MONTH?: number[];
  ACCUMULATED_DAYS_IN_MONTH_LEAP?: number[];
  APRIL?: number;
  AUGUST?: number;
  BASE_YEAR?: number;
  DAYS_IN_MONTH?: number[];
  DECEMBER?: number;
  FEBRUARY?: number;
  FIXED_DATES?: number[];
  FRIDAY?: number;
  JANUARY?: number;
  JULY?: number;
  JUNE?: number;
  MARCH?: number;
  MAY?: number;
  MONDAY?: number;
  NOVEMBER?: number;
  OCTOBER?: number;
  SATURDAY?: number;
  SEPTEMBER?: number;
  SUNDAY?: number;
  THURSDAY?: number;
  TUESDAY?: number;
  WEDNESDAY?: number;
}

export interface ClassDto {
  id?: string;
  name?: string;
}

export interface CreateEventRequestDTO {
  clientId?: string;
  customAttributes?: JsonObject;
  eventType?: string;
  headers?: { [key: string]: string };
  ip?: string;
  login?: string;
  module?: string;
  path?: string;
  userAgent?: string;
  userId?: string;
}

export interface CreateEventResponseDTO {
  eventId?: string;
}

export interface CreateGroupRequestDTO {
  classId?: string;
  externalId?: string;
  label?: string;
  name?: string;
  structureId?: string;
}

export interface CreateGroupResponseDTO {
  id?: string;
}

export interface Date {
  cdate?: Date;
  defaultCenturyStart?: number;
  fastTime?: number;
  gcal?: BaseCalendar;
  jcal?: BaseCalendar;
  serialVersionUID?: number;
  ttb?: number[];
  wtb?: string[];
}

export interface DeleteGroupRequestDTO {
  externalId?: string;
  id?: string;
}

export interface DeleteGroupResponseDTO {
  deleted?: boolean;
}

export interface DummyResponseDTO {
  jobId?: string;
  success?: boolean;
  userId?: string;
}

export interface FetchTranslationsRequestDTO {
  application?: string;
  headers?: { [key: string]: string };
  langAndDomain?: LangAndDomain;
}

export interface FetchTranslationsResponseDTO {
  translations?: { [key: string]: string };
}

export interface FindGroupByExternalIdRequestDTO {
  externalId?: string;
}

export interface FindGroupByExternalIdResponseDTO {
  group?: GroupDTO;
}

export interface FindSessionRequestDTO {
  cookies?: string;
  headers?: { [key: string]: string };
  params?: { [key: string]: string };
  path?: string;
  pathPrefix?: string;
  sessionId?: string;
}

export interface FindSessionResponseDTO {
  session?: SessionDto;
}

export interface GetResourcesRequestDTO {
  resourceIds?: string[];
}

export interface GetResourcesResponseDTO {
  resources?: ResourceInfoDTO[];
}

export interface GetUserDisplayNamesRequestDTO {
  userIds?: string[];
}

export interface GetUserDisplayNamesResponseDTO {
  userDisplayNames?: { [key: string]: string };
}

export interface GetUsersByIdsRequestDTO {
  userIds?: string[];
}

export interface GetUsersByIdsResponseDTO {
  users?: UserDTO[];
}

export interface GroupDTO {
  id?: string;
  name?: string;
}

export interface GroupDto {
  id?: string;
  name?: string;
}

export interface JsonObject {
  map?: { [key: string]: object };
}

export interface Kind {
}

export interface LangAndDomain {
  domain?: string;
  lang?: string;
}

export interface ListenAndAnswerDTO {
  jobId?: string;
  userId?: string;
}

export interface ListenOnlyDTO {
  timestamp?: number;
  userId?: string;
}

export interface LoadTestRequestDTO {
  delay?: number;
  payload?: string;
  responseSize?: number;
}

export interface LoadTestResponseDTO {
  elapsedTime?: number;
  payload?: string;
  startProcessingAt?: number;
  startWaitingAt?: number;
}

export interface RecreateCommunicationLinksRequestDTO {
  direction?: string;
  groupId?: string;
}

export interface RecreateCommunicationLinksResponseDTO {
  recreated?: boolean;
}

export interface RegisterTranslationFilesRequestDTO {
  application?: string;
  translationsByLanguage?: { [key: string]: { [key: string]: string } };
}

export interface RegisterTranslationFilesResponseDTO {
  application?: string;
  languagesCount?: number;
  translationsCount?: number;
}

export interface RemoveCommunicationLinksRequestDTO {
  direction?: string;
  groupId?: string;
}

export interface RemoveCommunicationLinksResponseDTO {
  removed?: boolean;
}

export interface RemoveGroupMemberRequestDTO {
  groupExternalId?: string;
  groupId?: string;
  userId?: string;
}

export interface RemoveGroupMemberResponseDTO {
  removed?: boolean;
}

export interface RemoveGroupSharesRequestDTO {
  application?: string;
  currentUserId?: string;
  groupExternalId?: string;
  groupId?: string;
  resourceId?: string;
}

export interface RemoveGroupSharesResponseDTO {
  shares?: SharesResponseDTO[];
}

export interface ResourceInfoDTO {
  authorId?: string;
  authorName?: string;
  creationDate?: Date;
  description?: string;
  id?: string;
  modificationDate?: Date;
  thumbnail?: string;
  title?: string;
}

export interface SecuredActionDTO {
  displayName?: string;
  name?: string;
  type?: string;
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

export interface SessionRecreationRequestDTO {
  refreshOnly?: boolean;
  sessionId?: string;
  userId?: string;
}

export interface SessionRecreationResponseDTO {
  session?: SessionDto;
}

export interface SharesResponseDTO {
  id?: string;
  kind?: Kind;
  permissions?: string[];
}

export interface StructureDto {
  id?: string;
  name?: string;
}

export interface UpdateGroupRequestDTO {
  externalId?: string;
  id?: string;
  name?: string;
}

export interface UpdateGroupResponseDTO {
  updated?: boolean;
}

export interface UpsertGroupSharesRequestDTO {
  application?: string;
  currentUserId?: string;
  groupId?: string;
  permissions?: string[];
  resourceId?: string;
}

export interface UpsertGroupSharesResponseDTO {
  shares?: SharesResponseDTO[];
}

export interface UserDTO {
  displayName?: string;
  functions?: { [key: string]: string[] };
  id?: string;
  profile?: string;
}

export interface UserFunctionDto {
  code?: string;
  scope?: string[];
}