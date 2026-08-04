import { absenceHandlers } from './handlers/absence-handlers';
import { configHandlers } from './handlers/config-handlers';
import { folderHandlers } from './handlers/folder-handlers';
import { messageHandlers } from './handlers/message-handlers';

/**
 * MSW Handlers
 * Mock HTTP methods for your own application
 */
export const handlers = [
  ...absenceHandlers,
  ...configHandlers,
  ...folderHandlers,
  ...messageHandlers,
];
