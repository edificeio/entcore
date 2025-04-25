import { setupWorker } from 'msw/browser';
import { tempHandlers } from './handlers/temp-handlers';

export const worker = setupWorker(...tempHandlers);
