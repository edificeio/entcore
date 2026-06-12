import { useBreakpoint, useOverlay } from '@edifice.io/react';
import { useEffect, useState } from 'react';

const NOTIFICATIONS_OPEN_KEY = 'timeline:notificationsOpen';

export const useNotificationsLayout = () => {
  const [isSidebarOpen, setIsSidebarOpen] = useState(false);
  const [isNotificationsOpen, setIsNotificationsOpen] = useState(
    () => localStorage.getItem(NOTIFICATIONS_OPEN_KEY) === 'true',
  );
  const { md, sm } = useBreakpoint();
  const { updateOverlayOpen } = useOverlay();

  const toggleNotifications = () => {
    setIsNotificationsOpen((prev) => !prev);
  };

  const closeNotifications = () => {
    setIsNotificationsOpen(false);
  };

  useEffect(() => {
    localStorage.setItem(NOTIFICATIONS_OPEN_KEY, String(isNotificationsOpen));
  }, [isNotificationsOpen]);

  // Close sidebar or overlay when resizing window to avoid inappropriate display
  useEffect(() => {
    if (md) {
      updateOverlayOpen(false);
      setIsSidebarOpen(isNotificationsOpen);
    } else if (sm) {
      updateOverlayOpen(isNotificationsOpen);
      setIsSidebarOpen(false);
    }
  }, [md, sm, isNotificationsOpen]);

  return {
    isSidebarOpen,
    toggleNotifications,
    closeNotifications,
  };
};
