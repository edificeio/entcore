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

export interface AppRegistrationRequestDTO {
  application?: AppRegistrationDTO;
  actions?: SecuredAction[];
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
export interface SecuredAction {
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
  actions?: SecuredAction[];
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
export interface SecuredAction {
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

