import { createFolderService } from './folderService';
import { createMessageService } from './messageService';

export const baseUrl = '/conversation';
export const folderService = createFolderService(baseUrl);
export const messageService = createMessageService(baseUrl);
