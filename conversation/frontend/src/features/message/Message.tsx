import clsx from 'clsx';
import { MessageActionDropdown } from '~/components/MessageActionDropdown/MessageActionDropdown';
import { MessageBody } from '~/components/MessageBody';
import { MessageHeader } from '~/features/message/message-header';
import { Message as MessageData } from '~/models';
import { MessageNavigation } from './MessageNavigation';

export interface MessageProps {
  message: MessageData;
  isPrint?: boolean;
}

export function Message({ message, isPrint }: MessageProps) {
  const className = clsx('d-flex flex-column gap-16', { 'my-16': isPrint });
  return (
    <article className={className}>
      {!isPrint && <MessageNavigation message={message} />}
      <div className="d-flex flex-column gap-16 p-16 ps-md-24 pt-0">
        <MessageHeader message={message} />
        <div className="ms-md-32">
          <MessageBody message={message} editMode={false} isPrint={isPrint} />
        </div>
        {!isPrint && (
          <footer className="d-print-none d-flex justify-content-end gap-12 pt-24 border-top ">
            <MessageActionDropdown
              message={message}
              actions={['reply', 'replyall', 'transfer']}
              className="gap-12"
            />
          </footer>
        )}
      </div>
    </article>
  );
}
