import { useDraggable } from '@dnd-kit/core';
import clsx from 'clsx';
import React from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { MessageMetadata } from '~/models';
import { useToggleUnreadMessagesFromQueryCache } from '~/services/queries/hooks/useToggleUnreadMessageFromQueryCache';
import { useScrollStore } from '~/store/scrollStore';
import { MessagePreview } from './MessagePreview/MessagePreview';
import './MessageItem.css';

interface MessageItemProps {
  message: MessageMetadata;
  checked: boolean;
  checkbox: JSX.Element | undefined;
  isDraggable?: boolean;
}
export function MessageItem({
  message,
  checked,
  checkbox,
  isDraggable = false,
}: MessageItemProps) {
  const navigate = useNavigate();
  // Keyboard users keep the "Move" toolbar action, so no draggable attributes are spread.
  const { listeners, setNodeRef, isDragging } = useDraggable({
    id: message.id,
    data: { messageId: message.id },
    disabled: !isDraggable,
  });
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

  const className = clsx('message-list-item', {
    'message-list-item-checked': checked,
    'message-list-item-unread': message.state !== 'DRAFT' && message.unread,
    'message-list-item-dragging': isDragging,
  });

  return (
    <div
      ref={setNodeRef}
      {...listeners}
      className={className}
      onClick={() => handleMessageClick(message)}
      onKeyUp={(event) => handleMessageKeyUp(event, message)}
      tabIndex={0}
      role="button"
      key={message.id}
      data-testid="message-item"
    >
      <div className="message-list-item-content">
        <div className="message-list-item-checkbox">{checkbox}</div>
        <MessagePreview message={message} />
      </div>
    </div>
  );
}
