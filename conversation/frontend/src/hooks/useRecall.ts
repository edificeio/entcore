import { useEdificeClient, useToast } from '@edifice.io/react';
import { createElement } from 'react';
import { MessageBase } from '~/models';
import { useRecallMessage } from '~/services';
import { useConfirmModalStore, useActionsStore } from '~/store';
import { useI18n } from './useI18n';
import { useRights } from './useRights';

export const useRecall = () => {
  const { t } = useI18n();
  const { success } = useToast();
  const { user } = useEdificeClient();
  const { canRecallMessages } = useRights();
  const { recallDelayMinutes } = useActionsStore.use.config();
  const { openModal } = useConfirmModalStore();
  const recallMessage = useRecallMessage();

  const canRecall = (message: MessageBase) => {
    return (
      canRecallMessages &&
      message.date &&
      message.state === 'SENT' &&
      message.from?.id === user?.userId &&
      message.date >= Date.now() - recallDelayMinutes * 60 * 1000
    );
  };

  const handleRecall = (message: MessageBase) => {
    openModal({
      id: 'recall-message-modal',
      size: 'sm',
      header: t('recall'),
      body: createElement('p', {}, t('conversation.recall.mail')),
      okText: t('conversation.recall.ok'),
      koText: t('cancel'),
      onSuccess: async () => {
        recallMessage.mutate(
          {
            messageId: message.id,
          },
          {
            onSuccess() {
              success(t('conversation.success.recall.mail'));
            },
          },
        );
      },
    });
  };

  return { canRecall, handleRecall };
};
