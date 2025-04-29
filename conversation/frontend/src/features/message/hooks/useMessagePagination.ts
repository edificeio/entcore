import { useSelectedFolder } from '~/hooks';
import { useFolderMessages } from '~/services';

export const useMessageNavigation = (messageId: string) => {
  const { folderId } = useSelectedFolder();
  const { messages, hasNextPage, fetchNextPage, isFetchingNextPage } =
    useFolderMessages(folderId!);

  if (!messages) {
    return { currentMessagePosition: undefined, totalMessagesCount: undefined };
  }

  const currentMessageIndex = messages?.findIndex(
    (msg) => msg.id === messageId,
  );
  const currentMessagePreview = messages[currentMessageIndex];
  if (
    currentMessageIndex === messages.length - 1 &&
    hasNextPage &&
    !isFetchingNextPage
  ) {
    fetchNextPage();
  }

  const currentMessagePosition =
    currentMessageIndex > -1 ? currentMessageIndex + 1 : undefined;
  const totalMessagesCount = currentMessagePreview?.count;

  const getMessageIdAtPosition = (position: number) => {
    if (position < 1 || position > messages.length) {
      return;
    }
    const message = messages[position - 1];
    return message.id;
  };

  return {
    currentMessagePosition,
    totalMessagesCount,
    getMessageIdAtPosition,
  };
};
