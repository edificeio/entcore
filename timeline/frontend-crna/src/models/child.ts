export interface Child {
  id: string;
  name: string;
  avatar: string;
  structureId: string;
  classIds: string[];
  /** Raw class codes (e.g. "4841$31"), used as a fallback when a class isn't found in the structure's class list. */
  classNames: string[];
}
