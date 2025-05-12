import { MessageActionDropdown } from '~/components/MessageActionDropdown/MessageActionDropdown';
import { MessageBody } from '~/components/MessageBody';
import { MessageHeader } from '~/features/message/message-header';
import { Message as MessageData } from '~/models';
import { MessageNavigation } from './MessageNavigation';

export interface MessageProps {
  message: MessageData;
}

export function Message({ message }: MessageProps) {
  return (
    <article className="d-flex flex-column gap-16">
      <MessageNavigation message={message} />
      <div className="d-flex flex-column gap-16 p-16 ps-md-24 pt-0">
        <MessageHeader message={message} />
        <div className="ms-md-48">
          <MessageBody message={message} editMode={false} />
        </div>
        <footer className="d-flex justify-content-end gap-12 pt-24 border-top">
          <MessageActionDropdown
            message={message}
            actions={['reply', 'replyall', 'transfer']}
            className="gap-12"
          />
        </footer>
      </div>
    </article>
  );
}
