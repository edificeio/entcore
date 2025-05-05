export interface AddGroupMemberRequestDTO {
  groupId?: string;
  groupExternalId?: string;
  userId?: string;
}


export interface AddGroupMemberResponseDTO {
  added?: boolean;
}

