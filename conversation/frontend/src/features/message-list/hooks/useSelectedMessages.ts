import { MessageMetadata } from '~/models';
import { useActionsStore } from '~/store/actions';

export default function useSelectedMessages(messages: MessageMetadata[]) {
  const selectedIds = useActionsStore.use.selectedMessageIds();
  return messages?.filter((message) => selectedIds.includes(message.id)) || [];
}
