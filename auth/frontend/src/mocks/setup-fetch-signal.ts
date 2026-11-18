import { afterAll, beforeAll } from 'vitest';

/**
 * jsdom replaces AbortController and AbortSignal with its own classes, while
 * fetch and Request stay Node's. Since Node 24 (undici 7), Request rejects a
 * jsdom signal ("Expected signal to be an instance of AbortSignal"): every
 * fetch given a signal fails in the tests, before MSW can answer it.
 *
 * This wrapper keeps the signal away from Node and applies it itself. It wraps
 * the fetch patched by MSW, so it must be installed after server.listen():
 * this file comes after setup.ts in setupFiles. Remove it once vitest makes the
 * jsdom signal compatible with Node's fetch (vitest 2.1 does not).
 */
let mswFetch: typeof fetch;

beforeAll(() => {
  mswFetch = globalThis.fetch;
  globalThis.fetch = (input, init) => {
    const signal = init?.signal;
    if (!signal) return mswFetch(input, init);
    if (signal.aborted) return Promise.reject(signal.reason);

    return new Promise((resolve, reject) => {
      const onAbort = () => reject(signal.reason);
      signal.addEventListener('abort', onAbort, { once: true });
      mswFetch(input, { ...init, signal: undefined })
        .then(resolve, reject)
        .finally(() => signal.removeEventListener('abort', onAbort));
    });
  };
});

afterAll(() => {
  globalThis.fetch = mswFetch;
});
