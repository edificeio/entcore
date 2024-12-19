import { queryOptions } from '@tanstack/react-query';
import { loadMessage } from '../api';

/**
 * Query options to load folders tree.
 * @returns queryOptions with key, fn, and selected data
 */
export const messageQueryOptions = (messageId: string) => {
  return queryOptions({
    queryKey: ['message'],
    queryFn: () => loadMessage(messageId),
  });
};
