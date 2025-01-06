import { QueryClient } from '@tanstack/react-query';
import { LoaderFunctionArgs } from 'react-router-dom';

import { messageQueryOptions } from '~/services';

export const loader =
  (queryClient: QueryClient) =>
  async ({ params /*, request*/ }: LoaderFunctionArgs) => {
    const queryMessage = messageQueryOptions.getById(
      params.messageId as string,
    );

    await Promise.all([queryClient.ensureQueryData(queryMessage)]);

    return null;
  };

export function Component() {
  return <>TODO</>;
}
