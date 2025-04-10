import { FormControl, Input, useDate, useDebounce } from '@edifice.io/react';
import { useEffect, useRef, useState } from 'react';
import { MessageActionDropDown } from '~/components/MessageActionDropDown/MessageActionDropDown';
import { MessageBody } from '~/components/MessageBody';
import { useI18n } from '~/hooks';
import { Message } from '~/models';
import { useConversationConfig, useCreateOrUpdateDraft } from '~/services';
import {
  useAppActions,
  useMessageUpdated,
  useMessageUpdatedNeedToSave,
} from '~/store';
import { MessageEditHeader } from './components/MessageEditHeader';

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
  const [dateKey, setDateKey] = useState(0);
  const { data: publicConfig } = useConversationConfig();

  useEffect(() => {
    setMessageUpdated(message);
    setContentKey((contentKey) => contentKey + 1);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  useEffect(() => {
    if (publicConfig && publicConfig['debounce-time-to-auto-save']) {
      debounceTimeToSave.current = publicConfig['debounce-time-to-auto-save'];
    }
  }, [publicConfig]);

  useEffect(() => {
    setMessageUpdated(message);
  }, [message, setMessageUpdated]);

  const handleSubjectChange = (subject: string) => {
    setSubject(subject);
    setMessageUpdated({ ...(messageUpdated || message), subject });
    setMessageUpdatedNeedToSave(true);
  };

  const handleMessageChange = (message: Message) => {
    setMessageUpdated({ ...(messageUpdated || message), body: message.body });
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

  useEffect(() => {
    const interval = setInterval(() => setDateKey((prev) => ++prev), 1000);

    return () => clearInterval(interval);
  }, []);

  return (
    <>
      {messageUpdated && (
        <div>
          <MessageEditHeader message={message} />
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
              <MessageActionDropDown message={messageUpdated} />
              {messageUpdated?.date && (
                <div className="caption fst-italic" key={dateKey}>
                  {fromNow(messageUpdated.date)}
                </div>
              )}
            </div>
          </div>
        </div>
      )}
    </>
  );
}
