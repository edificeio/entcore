import { FormControl, Input, useDate, useDebounce } from '@edifice.io/react';
import { useEffect, useRef, useState } from 'react';
import { MessageActionDropdown } from '~/components/MessageActionDropdown/MessageActionDropdown';
import { MessageBody } from '~/components/MessageBody';
import { useI18n } from '~/hooks';
import { useMessageIdAndAction } from '~/hooks/useMessageIdAndAction';
import { Message } from '~/models';
import { useConversationConfig, useCreateOrUpdateDraft } from '~/services';
import { useMessageActions, useMessageNeedToSave } from '~/store/messageStore';
import { MessageEditHeader } from './components/MessageEditHeader';

export function MessageEdit({ message }: { message?: Message }) {
  const { t } = useI18n();
  const [subject, setSubject] = useState(message?.subject);
  const messageUpdatedNeedSave = useMessageNeedToSave();
  const { setMessage, setMessageNeedToSave } = useMessageActions();
  const { fromNow } = useDate();
  const debounceTimeToSave = useRef(3000);
  const createOrUpdateDraft = useCreateOrUpdateDraft();
  const [dateKey, setDateKey] = useState(0);
  const { data: publicConfig } = useConversationConfig();
  const { action } = useMessageIdAndAction();
  const isTransferAction = action === 'transfer';

  const handleSubjectChange = (subject: string) => {
    if (!message) return null;
    setSubject(subject);
    setMessage({ ...message, subject });
    setMessageNeedToSave(true);
  };

  const handleMessageChange = (message: Message) => {
    setMessage(message);
    setMessageNeedToSave(true);
  };

  const messageUpdatedDebounced = useDebounce(
    message,
    debounceTimeToSave.current,
  );

  useEffect(() => {
    // Automatically create draft when this is a transfer action
    // so the attachments are transferred to the new message
    // and the user can edit the message
    if (message && !message.id && isTransferAction) {
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
    if (messageUpdatedDebounced && messageUpdatedNeedSave) {
      createOrUpdateDraft();
      setMessageNeedToSave(false);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [messageUpdatedDebounced]);

  return (
    <>
      {message && (
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
                message={message}
                appearance={{
                  dropdownVariant: 'outline',
                  mainButtonVariant: 'filled',
                  buttonColor: 'primary',
                }}
                className="gap-12"
              />
              {message?.date && (
                <div className="caption fst-italic" key={dateKey}>
                  {t('message.saved') + ' ' + fromNow(message.date)}
                </div>
              )}
            </div>
          </div>
        </div>
      )}
    </>
  );
}
