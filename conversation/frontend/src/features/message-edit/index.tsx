import { FormControl, Input, useDate } from '@edifice.io/react';
import { useEffect, useState } from 'react';
import { DisplayActionDropDown } from '~/components/DisplayActionDropDown';
import { MessageBody } from '~/components/MessageBody';
import { useI18n } from '~/hooks';
import { Message } from '~/models';
import { MessageHeaderEdit } from './MessageHeaderEdit';

export interface MessageEditProps {
  message: Message;
}

export function MessageEdit({ message }: MessageEditProps) {
  const { t } = useI18n();
  const [subject, setSubject] = useState(message.subject);
  const { fromNow } = useDate();

  useEffect(() => {
    message.subject = subject;
  }, [message, subject]);

  return (
    <div>
      <MessageHeaderEdit message={message} />
      <FormControl id="messageSubject" isRequired className="border-bottom">
        <Input
          placeholder={t('subject')}
          value={subject}
          size="lg"
          className="border-0"
          type="text"
          onChange={(e) => setSubject(e.target.value)}
        />
      </FormControl>
      <MessageBody message={message} editMode={true} />
      <div className="d-flex justify-content-end gap-12 pt-24 pe-16">
        <div className="d-flex align-items-end flex-column gap-16">
          <DisplayActionDropDown message={message} />
          {!!message.date && (
            <div className="caption fst-italic">{fromNow(message.date)}</div>
          )}
        </div>
      </div>
    </div>
  );
}
