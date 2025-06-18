import { createApplicationsService } from './applicationsService';
import { createMyAppsPreferencesService } from './myAppsPreferencesService';
import { createPreferencesService } from './preferencesService';

export const applicationsService = createApplicationsService();
export const preferencesService = createPreferencesService();
export const myAppsPreferencesService = createMyAppsPreferencesService();