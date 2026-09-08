import { HttpHandler, delay } from 'msw';

const RESPONSE_DELAY = 500;

export function applyDelayMiddleware(handlers: HttpHandler[]): HttpHandler[] {
  return handlers.map((handler) => {
    // @ts-ignore - hack to override the resolver
    const originalResolver = handler.resolver;

    // @ts-ignore - hack to override the resolver
    (handler as any).resolver = async (input: any) => {
      await delay(RESPONSE_DELAY);
      return originalResolver(input);
    };

    return handler;
  });
}
