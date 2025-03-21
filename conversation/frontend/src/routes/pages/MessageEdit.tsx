import { QueryClient } from '@tanstack/react-query';
import { useEffect } from 'react';
import { LoaderFunctionArgs, useParams } from 'react-router-dom';
import { MessageEdit } from '~/features/message-edit';
import { messageQueryOptions, useMessage } from '~/services';

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
  const { messageId } = useParams();

  const { data: message } = useMessage(messageId!);

  useEffect(() => {
    // Scroll to the top of the page
    window.scrollTo(0, 0);
  }, []);

  return <>{message && <MessageEdit message={message} />}</>;
}
