import { odeServices } from '@edifice.io/client';
import { FormControl, Input, useDate, useDebounce } from '@edifice.io/react';
import { useEffect, useRef, useState } from 'react';
import { DisplayActionDropDown } from '~/components/DisplayActionDropDown';
import { MessageBody } from '~/components/MessageBody';
import { useI18n } from '~/hooks';
import { Message } from '~/models';
import { useCreateOrUpdateDraft } from '~/services';
import { useAppActions, useMessageUpdated } from '~/store';
import { MessageHeaderEdit } from './MessageHeaderEdit';

export interface MessageEditProps {
  message: Message;
}

export function MessageEdit({ message }: MessageEditProps) {
  const { t } = useI18n();
  const [subject, setSubject] = useState(message.subject);
  const messageUpdated = useMessageUpdated();
  const { setMessageUpdated } = useAppActions();
  const { fromNow } = useDate();
  const debounceTimeToSave = useRef(5000);
  const createOrUpdateDraft = useCreateOrUpdateDraft();

  useEffect(() => {
    async function getConf() {
      return await odeServices.conf().getPublicConf('conversation');
    }
    const publicConf: any = getConf();
    if (publicConf && publicConf['debounce-time-to-auto-save']) {
      debounceTimeToSave.current = publicConf['debounce-time-to-auto-save'];
    }
  }, []);

  const handleSubjectChange = (subject: string) => {
    setSubject(subject);
    setMessageUpdated({ ...message, subject });
  };

  const handleMessageChange = (message: Message) => {
    setMessageUpdated({ ...message });
  };

  const messageUpdatedDebounced = useDebounce(
    messageUpdated,
    debounceTimeToSave.current,
  );

  useEffect(() => {
    if (messageUpdatedDebounced && messageUpdatedDebounced !== message) {
      createOrUpdateDraft();
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [messageUpdatedDebounced]);

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
          onChange={(e) => handleSubjectChange(e.target.value)}
        />
      </FormControl>
      <MessageBody
        message={message}
        editMode={true}
        onMessageChange={handleMessageChange}
      />
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
