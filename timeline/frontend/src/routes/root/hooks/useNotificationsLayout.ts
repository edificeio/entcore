import { useBreakpoint, useOverlay } from '@edifice.io/react';
import { useEffect, useState } from 'react';

export const useNotificationsLayout = () => {
  const [isSidebarOpen, setIsSidebarOpen] = useState(false);
  const { md, sm } = useBreakpoint();
  const { toggleOverlay, closeOverlay, openOverlay, isOverlayOpen } =
    useOverlay();

  const toggleNotifications = () => {
    if (md) {
      setIsSidebarOpen((prev) => !prev);
    } else {
      toggleOverlay();
    }
  };

  const closeNotifications = () => {
    if (md) {
      setIsSidebarOpen(false);
    } else {
      closeOverlay();
    }
  };

  // Close sidebar or overlay when resizing window to avoid inappropriate display
  useEffect(() => {
    if (md && isOverlayOpen) {
      closeOverlay();
      setIsSidebarOpen(true);
    } else if (sm && isSidebarOpen) {
      setIsSidebarOpen(false);
      openOverlay();
    }
  }, [md, sm]);

  return {
    isSidebarOpen,
    toggleNotifications,
    closeNotifications,
  };
};
