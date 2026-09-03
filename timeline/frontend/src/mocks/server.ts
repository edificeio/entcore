import { setupServer } from 'msw/node';
import { testHandlers } from './test-handlers';

export const server = setupServer(...testHandlers);
