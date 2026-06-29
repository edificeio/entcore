import { useBreakpoint, useOverlay } from '@edifice.io/react';
import { useEffect, useState } from 'react';

export const useNotificationsLayout = () => {
  const [isSidebarOpen, setIsSidebarOpen] = useState(false);
  const { md, sm } = useBreakpoint();
  const { toggleOverlay, updateOverlayOpen, isOverlayOpen } = useOverlay();

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
      updateOverlayOpen(false);
    }
  };

  // Close sidebar or overlay when resizing window to avoid inappropriate display
  useEffect(() => {
    if (md && isOverlayOpen) {
      updateOverlayOpen(false);
      setIsSidebarOpen(true);
    } else if (sm && isSidebarOpen) {
      setIsSidebarOpen(false);
      updateOverlayOpen(true);
    }
  }, [md, sm]);

  return {
    isSidebarOpen,
    toggleNotifications,
    closeNotifications,
  };
};
