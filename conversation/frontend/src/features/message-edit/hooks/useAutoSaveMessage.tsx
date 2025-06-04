import { useMessage, useMessageActions } from '~/store/messageStore';
import { useDebounce } from '@edifice.io/react';
import { useEffect, useRef } from 'react';
import { useConversationConfig, useCreateOrUpdateDraft } from '~/services';
import { useMessageNeedToSave } from '~/store/messageStore';

export const useAutoSaveMessage = () => {
  const { data: publicConfig } = useConversationConfig();
  const messageUpdatedNeedSave = useMessageNeedToSave();
  const createOrUpdateDraft = useCreateOrUpdateDraft();
  const { setMessageNeedToSave } = useMessageActions();
  const message = useMessage();
  const debounceTimeToSave = useRef(3000);
  const messageUpdatedNeedSaveDebounced = useDebounce(
    message,
    debounceTimeToSave.current,
  );

  useEffect(() => {
    if (publicConfig && publicConfig['debounce-time-to-auto-save']) {
      debounceTimeToSave.current = publicConfig['debounce-time-to-auto-save'];
    }
  }, [publicConfig]);

  useEffect(() => {
    if (messageUpdatedNeedSaveDebounced && messageUpdatedNeedSave) {
      console.log('Will Save:');
      createOrUpdateDraft();
      setMessageNeedToSave(false);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [messageUpdatedNeedSaveDebounced]);
};
