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
  authorizedActions?: OrgEntcoreBrokerApiDtoSessionActionDto[];
  classes?: OrgEntcoreBrokerApiDtoSessionClassDto[];
  groups?: OrgEntcoreBrokerApiDtoSessionGroupDto[];
  structures?: OrgEntcoreBrokerApiDtoSessionStructureDto[];
}
export interface OrgEntcoreBrokerApiDtoSessionActionDto {
  type?: string;
  name?: string;
  displayName?: string;
}
export interface OrgEntcoreBrokerApiDtoSessionClassDto {
  id?: string;
  name?: string;
}
export interface OrgEntcoreBrokerApiDtoSessionGroupDto {
  id?: string;
  name?: string;
}
export interface OrgEntcoreBrokerApiDtoSessionStructureDto {
  id?: string;
  name?: string;
}

