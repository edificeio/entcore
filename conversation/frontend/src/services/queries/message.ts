import { queryOptions } from '@tanstack/react-query';
import { messageService } from '..';

/**
 * Query options to load folders tree.
 * @returns queryOptions with key, fn, and selected data
 */
export const messageQueryOptions = (messageId: string) => {
  return queryOptions({
    queryKey: ['message'],
    queryFn: () => messageService.getById(messageId),
  });
};
