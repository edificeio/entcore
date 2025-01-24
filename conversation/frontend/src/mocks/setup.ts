/**
 * DO NOT MODIFY
 */

import '@testing-library/jest-dom';
import { RenderOptions, render } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { ReactElement } from 'react';
import { afterAll, afterEach, beforeAll } from 'vitest';
import '../i18n';
import { server } from './server';
import { MockedProviders } from '~/mocks/mockedProvider';

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

const user = userEvent.setup();

export const wrapper = MockedProviders;

const customRender = (
  ui: ReactElement,
  options?: Omit<RenderOptions & { path: string }, 'wrapper'>,
) => {
  return {
    user,
    ...render(ui, {
      wrapper: ({ children }: { children: React.ReactNode }) =>
        wrapper({
          initialEntries: options?.path ? [options.path] : undefined,
          children,
        }),
      ...options,
    }),
  };
};

export * from '@testing-library/react';
export { customRender as render };
