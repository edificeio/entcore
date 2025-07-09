import { useMessageStore } from '~/store/messageStore';
import { useDebounce } from '@edifice.io/react';
import { useEffect, useRef } from 'react';
import { useConversationConfig, useCreateOrUpdateDraft } from '~/services';

export const useAutoSaveMessage = () => {
  const { data: publicConfig } = useConversationConfig();
  const messageUpdatedNeedSave = useMessageStore.use.messageNeedToSave();
  const createOrUpdateDraft = useCreateOrUpdateDraft();
  const setMessageNeedToSave = useMessageStore.use.setMessageNeedToSave();
  const message = useMessageStore.use.message();
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
      createOrUpdateDraft();
      setMessageNeedToSave(false);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [messageUpdatedNeedSaveDebounced]);

  useEffect(() => {
    return () => {
      setMessageNeedToSave(false);
    };
  }, []);
};
