import { useEdificeClient } from '@edifice.io/react';
import { QueryClient } from '@tanstack/react-query';
import { useEffect } from 'react';
import { MessageEdit } from '~/features/message-edit';
import { Message as MessageModel } from '~/models';

export const loader = (_queryClient: QueryClient) => async () => {
  return null;
};

export function Component() {
  const { currentLanguage, user, userProfile } = useEdificeClient();

  useEffect(() => {
    // Scroll to the top of the page
    window.scrollTo(0, 0);
  }, []);

  const message: MessageModel = {
    id: '',
    body: '',
    language: currentLanguage,
    subject: '',
    from: {
      id: user?.userId || '',
      displayName: user?.username || '',
      profile: (userProfile || '') as string,
    },
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
    original_format_exists: false,
  };

  return <MessageEdit message={message} />;
}
