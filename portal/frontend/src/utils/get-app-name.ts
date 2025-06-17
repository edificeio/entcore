import { Application } from "~/models/application";

export const getAppName = (data: Application, t: (key: string) => string): string => {
  return data.prefix ? t(data.prefix.substring(1)) : t(data.displayName) || '';
};