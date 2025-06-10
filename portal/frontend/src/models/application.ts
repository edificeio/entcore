export type Application = {
  name: string;
  address: string;
  icon: string;
  target: string | null;
  displayName: string;
  display: boolean;
  prefix: string | null;
  casType: string | null;
  scope: string[];
  isExternal: boolean;
  category?: string;
  color?: string;
  libraries?: boolean;
  help?: helpType;
};

type helpType = {
  fr?: string | boolean;
  en?: string | boolean;
  es?: string | boolean;
};

export type ApplicationsResponse = {
  apps: Application[];
};