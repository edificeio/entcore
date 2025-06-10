import { useParams } from 'react-router-dom';
import { useFolderMessages } from '../folder';

/**
 * Hook to handle message mutations with folder context
 * @returns Object containing folder context and mutation utilities
 */
export const useMessageListOnMutate = () => {
  const { folderId } = useParams() as { folderId: string };
  const { messages, fetchNextPage, hasNextPage } = useFolderMessages(
    folderId,
    false,
  );

  const messageListOnMutate = async (id: string | string[]) => {
    const messageIds = typeof id === 'string' ? [id] : id;
    if (messages?.length === messageIds.length && hasNextPage) {
      await fetchNextPage();
    }
  };

  return {
    folderId,
    messages,
    messageListOnMutate,
  };
};
