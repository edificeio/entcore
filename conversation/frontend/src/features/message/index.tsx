import clsx from 'clsx';
import { DisplayActionDropDown } from '~/components/DisplayActionDropDown';
import { MessageHeader } from '~/features/message/MessageHeader';
import { Message as MessageData } from '~/models';
import { MessageBody } from './MessageBody';

export interface MessageProps {
  message: MessageData;
  editMode?: boolean;
}

export function Message({ message, editMode = false }: MessageProps) {
  const className = clsx(editMode ? '' : 'p-16 ps-md-24');
  return (
    <div className={className}>
      {editMode ? <></> : <MessageHeader message={message} />}
      <MessageBody message={message} editMode={editMode} />
      <div className="d-flex justify-content-end gap-12 pt-24 border-top">
        <DisplayActionDropDown message={message} />
      </div>
    </div>
  );
}
