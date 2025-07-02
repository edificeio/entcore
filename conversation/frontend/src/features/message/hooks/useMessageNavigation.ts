import { useSelectedFolder } from '~/hooks/useSelectedFolder';
import { MessageMetadata } from '~/models';
import { useFolderMessages } from '~/services';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { useScrollStore } from '~/store/scrollStore';

export const useMessageNavigation = (messageId?: string) => {
  const navigate = useNavigate();
  const { folderId } = useSelectedFolder();
  const { messages, hasNextPage, fetchNextPage, isFetchingNextPage } =
    useFolderMessages(folderId!, false);
  const savedScrollPosition = useScrollStore.use.savedScrollPosition();
  const [searchParams] = useSearchParams();

  if (!messages) {
    return { currentMessagePosition: undefined, totalMessagesCount: undefined };
  }

  const currentMessageIndex = messages.findIndex((msg) => msg.id === messageId);
  const currentMessagePreview = messages[currentMessageIndex];

  const currentMessagePosition =
    currentMessageIndex > -1 ? currentMessageIndex + 1 : undefined;
  const totalMessagesCount = currentMessagePreview?.count;

  const getMessageAtPosition = async (position: number) => {
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
      messageList = result?.data?.pages.flatMap(
        (page) => page,
      ) as MessageMetadata[];
    }

    const message = messageList[position - 1];
    return message;
  };

  const goBackToList = () => {
    navigate(
      {
        pathname: `../..`,
        search: searchParams.toString(),
      },
      {
        relative: 'path',
        state: {
          scrollPositionToRestore: savedScrollPosition,
        },
      },
    );
  };

  return {
    currentMessagePosition,
    totalMessagesCount,
    getMessageAtPosition,
    goBackToList,
  };
};
