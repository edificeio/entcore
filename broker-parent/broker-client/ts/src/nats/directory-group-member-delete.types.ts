export interface RemoveGroupMemberRequestDTO {
  groupId?: string;
  groupExternalId?: string;
  userId?: string;
}


export interface RemoveGroupMemberResponseDTO {
  removed?: boolean;
}

