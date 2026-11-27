import { useBreakpoint, useOverlay, useUser } from '@edifice.io/react';
import { useEffect, useState } from 'react';

/**
 * localStorage key holding the notifications panel state, per user:
 * `{ [userId]: true }`. Only open panels are stored (closed is the default),
 * so users sharing a device don't override each other's preference.
 */
const NOTIFICATIONS_OPEN_KEY = 'timeline:notificationsOpen';

type NotificationsOpenByUser = Record<string, true>;

const readNotificationsOpenByUser = (): NotificationsOpenByUser => {
  try {
    const parsed = JSON.parse(
      localStorage.getItem(NOTIFICATIONS_OPEN_KEY) ?? '{}',
    );
    // Legacy format was a plain 'true' / 'false' string shared by all users:
    // it can't be attributed to anyone, so it is ignored.
    return parsed && typeof parsed === 'object' && !Array.isArray(parsed)
      ? parsed
      : {};
  } catch {
    return {};
  }
};

const readNotificationsOpen = (userId?: string) =>
  !!userId && readNotificationsOpenByUser()[userId] === true;

const writeNotificationsOpen = (userId: string, isOpen: boolean) => {
  const openByUser = readNotificationsOpenByUser();
  if (isOpen) {
    openByUser[userId] = true;
  } else {
    delete openByUser[userId];
  }
  try {
    localStorage.setItem(NOTIFICATIONS_OPEN_KEY, JSON.stringify(openByUser));
  } catch {
    // Storage unavailable (private mode, quota): state is just not persisted
  }
};

export const useNotificationsLayout = ({
  isWidgetsPanelOpen,
}: {
  isWidgetsPanelOpen: boolean;
}) => {
  const { user } = useUser();
  const userId = user?.userId;
  const [isSidebarOpen, setIsSidebarOpen] = useState(false);
  const [isNotificationsOpen, setIsNotificationsOpen] = useState(() =>
    readNotificationsOpen(userId),
  );
  // The session may not be loaded on first render: restore the stored state
  // as soon as the user is known (state adjusted during render, before the
  // persistence effect below can write a stale value for this user).
  const [restoredForUserId, setRestoredForUserId] = useState(userId);
  if (restoredForUserId !== userId) {
    setRestoredForUserId(userId);
    setIsNotificationsOpen(readNotificationsOpen(userId));
  }
  const { md, sm } = useBreakpoint();
  const { updateOverlayOpen } = useOverlay();

  const toggleNotifications = () => {
    setIsNotificationsOpen((prev) => !prev);
  };

  const closeNotifications = () => {
    setIsNotificationsOpen(false);
  };

  useEffect(() => {
    if (userId) writeNotificationsOpen(userId, isNotificationsOpen);
  }, [userId, isNotificationsOpen]);

  // Single source of truth for the overlay: it is open for the widgets panel
  // (any breakpoint) or for notifications on small screens. Close sidebar or
  // overlay when resizing window to avoid inappropriate display.
  useEffect(() => {
    if (md) {
      updateOverlayOpen(isWidgetsPanelOpen);
      setIsSidebarOpen(isNotificationsOpen);
    } else if (sm) {
      updateOverlayOpen(isNotificationsOpen || isWidgetsPanelOpen);
      setIsSidebarOpen(false);
    }
  }, [md, sm, isNotificationsOpen, isWidgetsPanelOpen, updateOverlayOpen]);

  return {
    isSidebarOpen,
    toggleNotifications,
    closeNotifications,
  };
};
