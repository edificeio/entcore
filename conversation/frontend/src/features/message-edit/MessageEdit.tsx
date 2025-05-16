import { FormControl, Input, useDate, useDebounce } from '@edifice.io/react';
import { useEffect, useRef, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { MessageActionDropdown } from '~/components/MessageActionDropdown/MessageActionDropdown';
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
  const debounceTimeToSave = useRef(3000);
  const createOrUpdateDraft = useCreateOrUpdateDraft();
  const [dateKey, setDateKey] = useState(0);
  const { data: publicConfig } = useConversationConfig();
  const [searchParams] = useSearchParams();
  const transferMessageId = searchParams.get('transfer');

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
    setMessageUpdated(message);

    if (message && !message.id && transferMessageId) {
      createOrUpdateDraft();
    }

    const interval = setInterval(() => setDateKey((prev) => ++prev), 6000);

    return () => clearInterval(interval);
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

  useEffect(() => {
    if (messageUpdatedDebounced && messageUpdatedNeedSave) {
      createOrUpdateDraft();
      setMessageUpdatedNeedToSave(false);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [messageUpdatedDebounced]);

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
            message={message}
            editMode={true}
            onMessageChange={handleMessageChange}
          />
          <div className="d-flex justify-content-end gap-12 pt-24 pe-16">
            <div className="d-flex align-items-end flex-column gap-16">
              <MessageActionDropdown
                message={messageUpdated}
                appearance={{
                  dropdownVariant: 'outline',
                  mainButtonVariant: 'filled',
                  buttonColor: 'primary',
                }}
                className="gap-12"
              />
              {messageUpdated?.date && (
                <div className="caption fst-italic" key={dateKey}>
                  {t('message.saved') + ' ' + fromNow(messageUpdated.date)}
                </div>
              )}
            </div>
          </div>
        </div>
      )}
    </>
  );
}
