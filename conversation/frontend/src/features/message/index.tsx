import { DisplayActionDropDown } from '~/components/DisplayActionDropDown';
import { MessageBody } from '~/components/MessageBody';
import { MessageHeader } from '~/features/message/MessageHeader';
import { Message as MessageData } from '~/models';
import { MessageNavigation } from './MessageNavigation';

export interface MessageProps {
  message: MessageData;
}

export function Message({ message }: MessageProps) {
  return (
    <article className="d-flex gap-16 flex-column">
      <MessageNavigation message={message} />
      <div className="p-16 ps-md-24 pt-0">
        <MessageHeader message={message} />
        <div className="ps-md-48 my-md-24">
          <MessageBody message={message} editMode={false} />
        </div>
        <footer className="d-flex justify-content-end gap-12 pt-24 border-top">
          <DisplayActionDropDown
            message={message}
            actions={['reply', 'reply-all', 'transfer']}
          />
        </footer>
      </div>
    </article>
  );
}
