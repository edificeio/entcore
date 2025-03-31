export interface UpsertGroupSharesRequestDTO {
  currentUserId?: string;
  groupId?: string;
  permissions?: string[];
  resourceId?: string;
  application?: string;
}


export interface UpsertGroupSharesResponseDTO {
  shares?: SharesResponseDTO[];
}
export interface SharesResponseDTO {
  id?: string;
  kind?: Kind;
  permissions?: string[];
}
export interface Kind {}

export interface RemoveGroupSharesRequestDTO {
  currentUserId?: string;
  groupId?: string;
  groupExternalId?: string;
  resourceId?: string;
  application?: string;
}


export interface RemoveGroupSharesResponseDTO {
  shares?: SharesResponseDTO[];
}
export interface SharesResponseDTO {
  id?: string;
  kind?: Kind;
  permissions?: string[];
}
export interface Kind {}

export interface FindSessionRequestDTO {
  sessionId?: string;
  cookies?: string;
  headers?: {
    [k: string]: string;
  };
  params?: {
    [k: string]: string;
  };
  pathPrefix?: string;
  path?: string;
}


export interface FindSessionResponseDTO {
  session?: SessionDto;
}
export interface SessionDto {
  userId?: string;
  externalId?: string;
  firstName?: string;
  lastName?: string;
  username?: string;
  birthDate?: string;
  level?: string;
  type?: string;
  login?: string;
  email?: string;
  mobile?: string;
  authorizedActions?: ActionDto[];
  classes?: ClassDto[];
  groups?: GroupDto[];
  structures?: StructureDto[];
  functions?: UserFunctionDto[];
  superAdmin?: boolean;
}
export interface ActionDto {
  type?: string;
  name?: string;
  displayName?: string;
}
export interface ClassDto {
  id?: string;
  name?: string;
}
export interface GroupDto {
  id?: string;
  name?: string;
}
export interface StructureDto {
  id?: string;
  name?: string;
}
export interface UserFunctionDto {
  scope?: string[];
  code?: string;
}

export interface CreateGroupRequestDTO {
  externalId?: string;
  name?: string;
  classId?: string;
  structureId?: string;
}


export interface CreateGroupResponseDTO {
  id?: string;
}

export interface UpdateGroupRequestDTO {
  id?: string;
  externalId?: string;
  name?: string;
}


export interface UpdateGroupResponseDTO {
  updated?: boolean;
}

export interface DeleteGroupRequestDTO {
  id?: string;
  externalId?: string;
}


export interface DeleteGroupResponseDTO {
  deleted?: boolean;
}

export interface AddGroupMemberRequestDTO {
  groupId?: string;
  groupExternalId?: string;
  userId?: string;
}


export interface AddGroupMemberResponseDTO {
  added?: boolean;
}

export interface RemoveGroupMemberRequestDTO {
  groupId?: string;
  groupExternalId?: string;
  userId?: string;
}


export interface RemoveGroupMemberResponseDTO {
  removed?: boolean;
}

export interface FindGroupByExternalIdRequestDTO {
  externalId?: string;
}


export interface FindGroupByExternalIdResponseDTO {
  group?: GroupDTO;
}
export interface GroupDTO {
  id?: string;
  name?: string;
}

export interface GetUserDisplayNamesRequestDTO {
  userIds?: string[];
}


export interface GetUserDisplayNamesResponseDTO {
  userDisplayNames?: {
    [k: string]: string;
  };
}

export interface GetUsersByIdsRequestDTO {
  userIds?: string[];
}


export interface GetUsersByIdsResponseDTO {
  users?: UserDTO[];
}
export interface UserDTO {
  id?: string;
  displayName?: string;
  profile?: string;
  functions?: {
    [k: string]: string[];
  };
}

export interface AppRegistrationRequestDTO {
  application?: AppRegistrationDTO;
  actions?: SecuredActionDTO[];
}
export interface AppRegistrationDTO {
  name?: string;
  displayName?: string;
  appType?: string;
  icon?: string;
  address?: string;
  display?: boolean;
  prefix?: string;
  customProperties?: {
    [k: string]: {};
  };
}
export interface SecuredActionDTO {
  name?: string;
  displayName?: string;
  type?: string;
}


export interface AppRegistrationResponseDTO {
  success?: boolean;
  message?: string;
}

export interface AppRegistrationRequestDTO {
  application?: AppRegistrationDTO;
  actions?: SecuredActionDTO[];
}
export interface AppRegistrationDTO {
  name?: string;
  displayName?: string;
  appType?: string;
  icon?: string;
  address?: string;
  display?: boolean;
  prefix?: string;
  customProperties?: {
    [k: string]: {};
  };
}
export interface SecuredActionDTO {
  name?: string;
  displayName?: string;
  type?: string;
}


export interface AppRegistrationResponseDTO {
  success?: boolean;
  message?: string;
}

export interface FetchTranslationsRequestDTO {
  headers?: {
    [k: string]: string;
  };
  langAndDomain?: LangAndDomain;
  application?: string;
}
export interface LangAndDomain {
  lang?: string;
  domain?: string;
}


export interface FetchTranslationsResponseDTO {
  translations?: {
    [k: string]: string;
  };
}

export interface RegisterTranslationFilesRequestDTO {
  application?: string;
  translationsByLanguage?: {
    [k: string]: {
      [k: string]: string;
    };
  };
}


export interface RegisterTranslationFilesResponseDTO {
  application?: string;
  languagesCount?: number;
  translationsCount?: number;
}

export interface GetResourcesRequestDTO {
  resourceIds?: string[];
}


export interface GetResourcesResponseDTO {
  resources?: ResourceInfoDTO[];
}
export interface ResourceInfoDTO {
  id?: string;
  title?: string;
  description?: string;
  thumbnail?: string;
  authorName?: string;
  authorId?: string;
  creationDate?: Date;
  modificationDate?: Date2;
}
export interface Date {
  gcal?: BaseCalendar;
  jcal?: BaseCalendar1;
  fastTime?: number;
  cdate?: Date1;
  defaultCenturyStart?: number;
  serialVersionUID?: number;
  wtb?: string[];
  ttb?: number[];
}
export interface BaseCalendar {
  JANUARY?: number;
  FEBRUARY?: number;
  MARCH?: number;
  APRIL?: number;
  MAY?: number;
  JUNE?: number;
  JULY?: number;
  AUGUST?: number;
  SEPTEMBER?: number;
  OCTOBER?: number;
  NOVEMBER?: number;
  DECEMBER?: number;
  SUNDAY?: number;
  MONDAY?: number;
  TUESDAY?: number;
  WEDNESDAY?: number;
  THURSDAY?: number;
  FRIDAY?: number;
  SATURDAY?: number;
  BASE_YEAR?: number;
  FIXED_DATES?: number[];
  DAYS_IN_MONTH?: number[];
  ACCUMULATED_DAYS_IN_MONTH?: number[];
  ACCUMULATED_DAYS_IN_MONTH_LEAP?: number[];
}
export interface BaseCalendar1 {
  JANUARY?: number;
  FEBRUARY?: number;
  MARCH?: number;
  APRIL?: number;
  MAY?: number;
  JUNE?: number;
  JULY?: number;
  AUGUST?: number;
  SEPTEMBER?: number;
  OCTOBER?: number;
  NOVEMBER?: number;
  DECEMBER?: number;
  SUNDAY?: number;
  MONDAY?: number;
  TUESDAY?: number;
  WEDNESDAY?: number;
  THURSDAY?: number;
  FRIDAY?: number;
  SATURDAY?: number;
  BASE_YEAR?: number;
  FIXED_DATES?: number[];
  DAYS_IN_MONTH?: number[];
  ACCUMULATED_DAYS_IN_MONTH?: number[];
  ACCUMULATED_DAYS_IN_MONTH_LEAP?: number[];
}
export interface Date1 {
  cachedYear?: number;
  cachedFixedDateJan1?: number;
  cachedFixedDateNextJan1?: number;
}
export interface Date2 {
  gcal?: BaseCalendar2;
  jcal?: BaseCalendar3;
  fastTime?: number;
  cdate?: Date3;
  defaultCenturyStart?: number;
  serialVersionUID?: number;
  wtb?: string[];
  ttb?: number[];
}
export interface BaseCalendar2 {
  JANUARY?: number;
  FEBRUARY?: number;
  MARCH?: number;
  APRIL?: number;
  MAY?: number;
  JUNE?: number;
  JULY?: number;
  AUGUST?: number;
  SEPTEMBER?: number;
  OCTOBER?: number;
  NOVEMBER?: number;
  DECEMBER?: number;
  SUNDAY?: number;
  MONDAY?: number;
  TUESDAY?: number;
  WEDNESDAY?: number;
  THURSDAY?: number;
  FRIDAY?: number;
  SATURDAY?: number;
  BASE_YEAR?: number;
  FIXED_DATES?: number[];
  DAYS_IN_MONTH?: number[];
  ACCUMULATED_DAYS_IN_MONTH?: number[];
  ACCUMULATED_DAYS_IN_MONTH_LEAP?: number[];
}
export interface BaseCalendar3 {
  JANUARY?: number;
  FEBRUARY?: number;
  MARCH?: number;
  APRIL?: number;
  MAY?: number;
  JUNE?: number;
  JULY?: number;
  AUGUST?: number;
  SEPTEMBER?: number;
  OCTOBER?: number;
  NOVEMBER?: number;
  DECEMBER?: number;
  SUNDAY?: number;
  MONDAY?: number;
  TUESDAY?: number;
  WEDNESDAY?: number;
  THURSDAY?: number;
  FRIDAY?: number;
  SATURDAY?: number;
  BASE_YEAR?: number;
  FIXED_DATES?: number[];
  DAYS_IN_MONTH?: number[];
  ACCUMULATED_DAYS_IN_MONTH?: number[];
  ACCUMULATED_DAYS_IN_MONTH_LEAP?: number[];
}
export interface Date3 {
  cachedYear?: number;
  cachedFixedDateJan1?: number;
  cachedFixedDateNextJan1?: number;
}

