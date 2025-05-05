export interface RemoveGroupSharesRequestDTO {
  currentUserId?: string;
  groupId?: string;
  groupExternalId?: string;
  resourceId?: string;
  application?: string;
}


export interface RemoveGroupSharesResponseDTO {
  shares?: OrgEntcoreBrokerApiDtoSharesSharesResponseDTO[];
}
export interface OrgEntcoreBrokerApiDtoSharesSharesResponseDTO {
  id?: string;
  kind?: OrgEntcoreBrokerApiDtoSharesSharesResponseDTOKind;
  permissions?: string[];
}
export interface OrgEntcoreBrokerApiDtoSharesSharesResponseDTOKind {}

