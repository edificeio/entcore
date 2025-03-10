import {
  ConversationHistoryNodeView,
  ConversationHistoryRenderer,
  Editor,
} from '@edifice.io/react/editor';
import { DisplayActionDropDown } from '~/components/DisplayActionDropDown';
import { MessageAttachments } from '~/features/message/MessageAttachments';
import { MessageHeader } from '~/features/message/MessageHeader';
import { Message as MessageData } from '~/models';

export function Message({ message }: { message: MessageData }) {
  const extensions = [ConversationHistoryNodeView(ConversationHistoryRenderer)];

  return (
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
      <div className="d-flex justify-content-end gap-12">
        <DisplayActionDropDown message={message} />
      </div>
    </div>
  );
}
