import { Model } from "entcore-toolkit";

export type UserPostisionSource = "MANUAL" | "AAF" | "CSV";
export interface UserPositionCreation {
  name: string;
  structureId: string;
}
export interface UserPositionElementQuery {
  /**
   * Keep only results having this criteria.
   */
  prefix?: string;

  /**
   *  Restrict results to userPosition in existing in the structure list.
   */
  structureIds?: string[];
}

export interface UserPosition {
  id?: string;
  name?: string;
  source: UserPostisionSource;
}
