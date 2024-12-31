import { factory as createFolderService } from './folderServiceFactory';
import { factory as createMessageService } from './messageServiceFactory';

export const baseUrl = '/conversation';
export const folderService = createFolderService(baseUrl);
export const messageService = createMessageService(baseUrl);
