import clsx from 'clsx';
import React from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { MessageMetadata } from '~/models';
import { useToggleUnreadMessagesFromQueryCache } from '~/services/queries/hooks/useToggleUnreadMessageFromQueryCache';
import { useScrollStore } from '~/store/scrollStore';
import { MessagePreview } from './MessagePreview/MessagePreview';

interface MessageItemProps {
  message: MessageMetadata;
  checked: boolean;
  checkbox: JSX.Element | undefined;
}
export function MessageItem({ message, checked, checkbox }: MessageItemProps) {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const { toggleUnreadMessagesFromQueryCache } =
    useToggleUnreadMessagesFromQueryCache();

  const currentScrollPosition = useScrollStore.use.currentScrollPosition();
  const setSavedScrollPosition = useScrollStore.use.setSavedScrollPosition();
  const handleMessageKeyUp = (
    event: React.KeyboardEvent<HTMLDivElement>,
    message: MessageMetadata,
  ) => {
    if (event.key === 'Enter') {
      handleMessageClick(message);
    }
  };

  const handleMessageClick = (message: MessageMetadata) => {
    toggleUnreadMessagesFromQueryCache([message], false);
    setSavedScrollPosition(currentScrollPosition);
    navigate({
      pathname: `message/${message.id}`,
      search: searchParams.toString(),
    });
  };

  const className = clsx(
    'd-flex message-list-item gap-24 px-16 py-12 mb-2 overflow-hidden',
    {
      'bg-secondary-200': checked,
      'fw-bold bg-primary-200 gray-800':
        message.state !== 'DRAFT' && message.unread,
    },
  );

  return (
    <div
      className={className}
      onClick={() => handleMessageClick(message)}
      onKeyUp={(event) => handleMessageKeyUp(event, message)}
      tabIndex={0}
      role="button"
      key={message.id}
      data-testid="message-item"
    >
      <div className="d-flex align-items-center gap-12 g-col-3 flex-fill overflow-hidden">
        <div className="ps-lg-8">{checkbox}</div>
        <MessagePreview message={message} />
      </div>
    </div>
  );
}
