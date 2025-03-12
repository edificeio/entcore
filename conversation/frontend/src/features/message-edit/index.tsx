import { DisplayActionDropDown } from '~/components/DisplayActionDropDown';
import { MessageBody } from '~/components/MessageBody';
import { Message as MessageData } from '~/models';
import { MessageHeaderEdit } from './MessageHeaderEdit';

export interface MessageEditProps {
  message: MessageData;
}

export function MessageEdit({ message }: MessageEditProps) {
  return (
    <>
      <MessageHeaderEdit message={message} />
      <MessageBody message={message} editMode={true} />
      <div className="d-flex justify-content-end gap-12 pt-24 pe-16">
        <DisplayActionDropDown message={message} />
      </div>
    </>
  );
}
