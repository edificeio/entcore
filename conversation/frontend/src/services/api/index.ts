import { createAttachmentService } from './attachmentService';
import { createConfigService } from './configService';
import { createFolderService } from './folderService';
import { createMessageService } from './messageService';
import { createUserService } from './userService';

export const baseUrl = '/conversation';
export const configService = createConfigService(baseUrl);
export const folderService = createFolderService(baseUrl);
export const messageService = createMessageService(baseUrl);
export const attachmentService = createAttachmentService(baseUrl);
export const userService = createUserService();
