export interface UpsertGroupSharesRequestDTO {
  currentUserId?: string;
  groupId?: string;
  permissions?: string[];
  resourceId?: string;
  application?: string;
}


export interface UpsertGroupSharesResponseDTO {
  shares?: OrgEntcoreBrokerApiDtoSharesSharesResponseDTO[];
}
export interface OrgEntcoreBrokerApiDtoSharesSharesResponseDTO {
  id?: string;
  kind?: OrgEntcoreBrokerApiDtoSharesSharesResponseDTOKind;
  permissions?: string[];
}
export interface OrgEntcoreBrokerApiDtoSharesSharesResponseDTOKind {}

