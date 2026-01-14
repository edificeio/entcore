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
  appName?: string;
  isFavorite?: boolean;
  version?: APP_VERSION;
};

export type APP_VERSION =
  | 'BETA'
  | 'ALPHA'
  | 'STABLE'
  | 'DEPRECATED'
  | 'EXPERIMENTAL';

type helpType = {
  [lang: string]: string | null;
};

export type ApplicationsResponse = {
  apps: Application[];
};
