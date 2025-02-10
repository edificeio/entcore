import {
  ConversationHistoryNodeView,
  ConversationHistoryRenderer,
  Editor,
} from '@edifice.io/react/editor';
import { QueryClient } from '@tanstack/react-query';
import { useEffect } from 'react';
import { LoaderFunctionArgs, useParams } from 'react-router-dom';
import { MessageAttachments } from '~/features/Message/message-attachments';
import { MessageHeader } from '~/features/Message/message-header';

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

  const extensions = [ConversationHistoryNodeView(ConversationHistoryRenderer)];

  useEffect(() => {
    // Scroll to the top of the page
    window.scrollTo(0, 0);
  }, []);

  return (
    <>
      {message && (
        <div className="p-16">
          <MessageHeader message={message} />
          <div className="p-md-48">
            <Editor
              content={message.body}
              mode="read"
              variant="ghost"
              extensions={extensions}
            />
            {!!message.attachments?.length && (
              <MessageAttachments
                attachments={message.attachments}
                messageId={message.id}
              />
            )}
          </div>
        </div>
      )}
    </>
  );
}
