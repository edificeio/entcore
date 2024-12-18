/**
 * DO NOT MODIFY
 */

import '@testing-library/jest-dom';
import { RenderOptions, render } from '@testing-library/react';
import { ReactElement } from 'react';
import { afterAll, afterEach, beforeAll } from 'vitest';
import '../i18n';
import { Providers } from '../providers';
import { server } from './server';

// Enable API mocking before tests.
beforeAll(() =>
  server.listen({
    onUnhandledRequest: 'bypass',
  }),
);

// Reset any request handlers that are declared as a part of our tests
// (i.e. for testing one-time error scenarios)
afterEach(() => server.resetHandlers());

// Disable API mocking after the tests are done.
afterAll(() => server.close());

const customRender = (
  ui: ReactElement,
  options?: Omit<RenderOptions, 'wrapper'>,
) => render(ui, { wrapper: Providers, ...options });

export * from '@testing-library/react';
export { customRender as render };

