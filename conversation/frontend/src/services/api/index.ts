import { createFolderService } from './folderService';
import { createMessageService } from './messageService';
import { createAttachmentService } from './attachmentService';

export const baseUrl = '/conversation';
export const folderService = createFolderService(baseUrl);
export const messageService = createMessageService(baseUrl);
export const attachmentService = createAttachmentService(baseUrl);