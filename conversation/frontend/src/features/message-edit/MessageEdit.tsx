import { FormControl, Input } from '@edifice.io/react';
import { useEffect, useState } from 'react';
import { MessageActionDropdown } from '~/components/MessageActionDropdown/MessageActionDropdown';
import { MessageBody } from '~/components/MessageBody';
import { useI18n } from '~/hooks/useI18n';
import { useMessageIdAndAction } from '~/hooks/useMessageIdAndAction';
import { Message } from '~/models';
import { useCreateOrUpdateDraft } from '~/services';
import { useMessageActions } from '~/store/messageStore';
import { MessageEditHeader } from './components/MessageEditHeader';
import { MessageSaveDate } from './components/MessageSaveDate';
import { useAutoSaveMessage } from './hooks/useAutoSaveMessage';

export function MessageEdit({ message }: { message?: Message }) {
  console.log('message:', message);
  const { t } = useI18n();
  const [subject, setSubject] = useState(message?.subject);
  const { setMessage, setMessageNeedToSave } = useMessageActions();
  const createOrUpdateDraft = useCreateOrUpdateDraft();
  useAutoSaveMessage();
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

  useEffect(() => {
    // Automatically create draft when this is a transfer action
    // so the attachments are transferred to the new message
    // and the user can edit the message
    if (message && !message.id && isTransferAction) {
      createOrUpdateDraft();
    }
  }, []);

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
          <div className="d-print-none d-flex justify-content-end gap-12 pt-24 pe-16">
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
              <MessageSaveDate />
            </div>
          </div>
        </div>
      )}
    </>
  );
}
