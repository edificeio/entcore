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

