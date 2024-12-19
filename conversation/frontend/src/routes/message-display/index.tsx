import { QueryClient } from '@tanstack/react-query';
import { LoaderFunctionArgs } from 'react-router-dom';

import { messageQueryOptions } from '~/services/queries';

export const loader =
  (queryClient: QueryClient) =>
  async ({ params /*, request*/ }: LoaderFunctionArgs) => {
    const queryMessage = messageQueryOptions(params.messageId as string);

    await Promise.all([queryClient.fetchQuery(queryMessage)]);

    return null;
  };

export function Component() {
  return <></>;
}
