import { useBreakpoint, useOverlay } from '@edifice.io/react';
import { useEffect, useState } from 'react';

const NOTIFICATIONS_OPEN_KEY = 'timeline:notificationsOpen';

export const useNotificationsLayout = ({
  isWidgetsPanelOpen,
}: {
  isWidgetsPanelOpen: boolean;
}) => {
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
