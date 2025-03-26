import { odeServices } from '@edifice.io/client';
import { FormControl, Input, useDate, useDebounce } from '@edifice.io/react';
import { useEffect, useRef, useState } from 'react';
import { DisplayActionDropDown } from '~/components/DisplayActionDropDown';
import { MessageBody } from '~/components/MessageBody';
import { useI18n } from '~/hooks';
import { Message } from '~/models';
import { useCreateOrUpdateDraft } from '~/services';
import {
  useAppActions,
  useMessageUpdated,
  useMessageUpdatedNeedToSave,
} from '~/store';
import { MessageHeaderEdit } from './MessageHeaderEdit';

export interface MessageEditProps {
  message: Message;
}

export function MessageEdit({ message }: MessageEditProps) {
  const { t } = useI18n();
  const [subject, setSubject] = useState(message.subject);
  const messageUpdated = useMessageUpdated();
  const messageUpdatedNeedSave = useMessageUpdatedNeedToSave();
  const { setMessageUpdated, setMessageUpdatedNeedToSave } = useAppActions();
  const { fromNow } = useDate();
  const debounceTimeToSave = useRef(5000);
  const createOrUpdateDraft = useCreateOrUpdateDraft();
  const [contentKey, setContentKey] = useState(0);

  useEffect(() => {
    odeServices
      .conf()
      .getPublicConf('conversation')
      .then((publicConf: any) => {
        if (publicConf && publicConf['debounce-time-to-auto-save']) {
          debounceTimeToSave.current = publicConf['debounce-time-to-auto-save'];
        }
      });
    setMessageUpdated(message);
    setContentKey((contentKey) => contentKey + 1);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  useEffect(() => {
    setMessageUpdated(message);
  }, [message, setMessageUpdated]);

  const handleSubjectChange = (subject: string) => {
    setSubject(subject);
    setMessageUpdated({ ...message, subject });
    setMessageUpdatedNeedToSave(true);
  };

  const handleMessageChange = (message: Message) => {
    setMessageUpdated({ ...message });
    setMessageUpdatedNeedToSave(true);
  };

  const messageUpdatedDebounced = useDebounce(
    messageUpdated,
    debounceTimeToSave.current,
  );

  useEffect(() => {
    if (messageUpdatedDebounced && messageUpdatedNeedSave) {
      createOrUpdateDraft();
      setMessageUpdatedNeedToSave(false);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [messageUpdatedDebounced]);

  if (!messageUpdated) {
    return null;
  }
  return (
    <div>
      <MessageHeaderEdit message={messageUpdated} />
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
        key={contentKey}
        message={message}
        editMode={true}
        onMessageChange={handleMessageChange}
      />
      <div className="d-flex justify-content-end gap-12 pt-24 pe-16">
        <div className="d-flex align-items-end flex-column gap-16">
          <DisplayActionDropDown message={messageUpdated} />
          {!!messageUpdated?.date && (
            <div className="caption fst-italic">
              {fromNow(messageUpdated.date)}
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
