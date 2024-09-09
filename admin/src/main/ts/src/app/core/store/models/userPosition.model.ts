export type UserPositionSource = "MANUAL" | "AAF" | "CSV";
export interface UserPositionCreation {
  name: string;
  structureId: string;
}
export interface UserPositionElementQuery {
  /**
   * Keep only results having this criteria.
   */
  content?: string;

  /**
   *  Restrict results to userPosition in existing in the structure list.
   */
  structureId?: string;
}

export interface UserPosition {
  id?: string;
  name?: string;
  source: UserPositionSource;
  structureId?: string;
}
