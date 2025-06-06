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
};

export type ApplicationsResponse = {
  apps: Application[];
};