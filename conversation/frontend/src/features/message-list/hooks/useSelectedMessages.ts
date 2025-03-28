import { MessageMetadata } from '~/models';
import { useSelectedMessageIds } from '~/store';

export default function useSelectedMessages(messages: MessageMetadata[]) {
  const selectedIds = useSelectedMessageIds();
  return messages?.filter((message) => selectedIds.includes(message.id)) || [];
}
