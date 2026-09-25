import { useOverlay } from '@edifice.io/react';
import { useState } from 'react';

export const useWidgetsPanelLayout = ({
  toggleNotifications,
  closeNotifications,
}: {
  toggleNotifications: () => void;
  closeNotifications: () => void;
}) => {
  const { updateOverlayOpen } = useOverlay();
  const [isWidgetsPanelOpen, setIsWidgetsPanelOpen] = useState(false);

  // The overlay only has one content slot, shared with notifications — keep
  // the two mutually exclusive rather than stacking them.
  const openWidgetsPanel = () => {
    closeNotifications();
    setIsWidgetsPanelOpen(true);
    updateOverlayOpen(true);
  };

  const closeWidgetsPanel = () => {
    setIsWidgetsPanelOpen(false);
    updateOverlayOpen(false);
  };

  const handleToggleNotifications = () => {
    setIsWidgetsPanelOpen(false);
    toggleNotifications();
  };

  return {
    isWidgetsPanelOpen,
    openWidgetsPanel,
    closeWidgetsPanel,
    handleToggleNotifications,
  };
};
