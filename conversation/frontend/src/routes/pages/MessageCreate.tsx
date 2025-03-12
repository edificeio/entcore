import { useEdificeClient } from '@edifice.io/react';
import { QueryClient } from '@tanstack/react-query';
import { useEffect } from 'react';
import { MessageEdit } from '~/features/message-edit';
import { Message as MessageModel } from '~/models';

export const loader = (_queryClient: QueryClient) => async () => {
  return null;
};

export function Component() {
  const { currentLanguage } = useEdificeClient();

  useEffect(() => {
    // Scroll to the top of the page
    window.scrollTo(0, 0);
  }, []);

  const message: MessageModel = {
    id: '',
    body: '',
    language: currentLanguage,
    subject: '',
    to: {
      users: [],
      groups: [],
    },
    cc: {
      users: [],
      groups: [],
    },
    cci: {
      users: [],
      groups: [],
    },
    response: false,
    forwarded: false,
    state: 'DRAFT',
    attachments: [],
  };

  return (
    <>
      <MessageEdit message={message} />
    </>
  );
}
