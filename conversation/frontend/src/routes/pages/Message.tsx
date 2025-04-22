import { useEdificeClient } from '@edifice.io/react';
import { QueryClient } from '@tanstack/react-query';
import { useEffect, useState } from 'react';
import { LoaderFunctionArgs, useParams } from 'react-router-dom';
import { Message } from '~/features/message';
import { MessageEdit } from '~/features/message-edit/MessageEdit';
import { useSelectedFolder } from '~/hooks';

import { messageQueryOptions, useMessage } from '~/services';

export const loader =
  (queryClient: QueryClient) =>
  async ({ params /*, request*/ }: LoaderFunctionArgs) => {
    const queryMessage = messageQueryOptions.getById(
      params.messageId as string,
    );
    if (params.messageId) {
      await Promise.all([queryClient.ensureQueryData(queryMessage)]);
    }

    return null;
  };

export function Component() {
  const { messageId } = useParams();
  const { folderId } = useSelectedFolder();
  const [editMode, setEditMode] = useState(false);
  const { currentLanguage, user, userProfile } = useEdificeClient();

  const { data } = useMessage(messageId!);
  let message = data;

  if (!message) {
    message = {
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
  }

  if (folderId === 'draft' && message?.state === 'DRAFT') {
    setEditMode(true);
  }

  useEffect(() => {
    // Scroll to the top of the page
    window.scrollTo(0, 0);
  }, []);

  return (
    <>
      {editMode ? (
        <MessageEdit message={message} />
      ) : (
        <Message message={message} />
      )}
    </>
  );
}
