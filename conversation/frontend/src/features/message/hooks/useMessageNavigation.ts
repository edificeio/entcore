import { useSelectedFolder } from '~/hooks';
import { MessageMetadata } from '~/models';
import { useFolderMessages } from '~/services';

export const useMessageNavigation = (messageId: string) => {
  const { folderId } = useSelectedFolder();
  const { messages, hasNextPage, fetchNextPage, isFetchingNextPage } =
    useFolderMessages(folderId!, false);

  if (!messages) {
    return { currentMessagePosition: undefined, totalMessagesCount: undefined };
  }

  const currentMessageIndex = messages.findIndex((msg) => msg.id === messageId);
  const currentMessagePreview = messages[currentMessageIndex];

  const currentMessagePosition =
    currentMessageIndex > -1 ? currentMessageIndex + 1 : undefined;
  const totalMessagesCount = currentMessagePreview?.count;

  const getMessageIdAtPosition = async (position: number) => {
    let messageList: MessageMetadata[] = messages;
    if (position < 1 || position > totalMessagesCount) {
      return;
    }
    if (
      position === messages.length + 1 &&
      hasNextPage &&
      !isFetchingNextPage
    ) {
      const result = await fetchNextPage();
      messageList = result.data?.pages.flatMap(
        (page) => page,
      ) as MessageMetadata[];
    }

    const message = messageList[position - 1];
    return message.id;
  };

  return {
    currentMessagePosition,
    totalMessagesCount,
    getMessageIdAtPosition,
  };
};
