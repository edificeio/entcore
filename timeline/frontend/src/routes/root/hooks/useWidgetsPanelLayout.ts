import { useBreakpoint } from '@edifice.io/react';
import { useEffect, useRef, useState } from 'react';

// Matches the overlay slide-out transition (0.3s) in @edifice.io/bootstrap
const OVERLAY_TRANSITION_MS = 300;

export const useWidgetsPanelLayout = ({
  isWidgetsPanelOpen,
  setIsWidgetsPanelOpen,
  toggleNotifications,
  closeNotifications,
}: {
  isWidgetsPanelOpen: boolean;
  setIsWidgetsPanelOpen: (isOpen: boolean) => void;
  toggleNotifications: () => void;
  closeNotifications: () => void;
}) => {
  const { md } = useBreakpoint();
  // Stays true during the slide-out so the panel doesn't vanish mid-transition
  const [isWidgetsPanelMounted, setIsWidgetsPanelMounted] = useState(false);
  const unmountTimer = useRef<ReturnType<typeof setTimeout>>();

  const clearUnmountTimer = () => clearTimeout(unmountTimer.current);

  useEffect(() => clearUnmountTimer, []);

  // Below 'md' notifications share the overlay's single content slot with the
  // widgets panel, so they must be closed. On desktop they live in the right
  // sidebar and the panel simply opens over them. The overlay open state
  // itself is synced in useNotificationsLayout.
  const openWidgetsPanel = () => {
    if (!md) closeNotifications();
    clearUnmountTimer();
    setIsWidgetsPanelMounted(true);
    setIsWidgetsPanelOpen(true);
  };

  const closeWidgetsPanel = () => {
    setIsWidgetsPanelOpen(false);
    clearUnmountTimer();
    unmountTimer.current = setTimeout(
      () => setIsWidgetsPanelMounted(false),
      OVERLAY_TRANSITION_MS,
    );
  };

  const handleToggleNotifications = () => {
    // The overlay stays open and its content swaps right away: no slide-out
    clearUnmountTimer();
    setIsWidgetsPanelMounted(false);
    setIsWidgetsPanelOpen(false);
    toggleNotifications();
  };

  return {
    isWidgetsPanelOpen,
    isWidgetsPanelMounted,
    openWidgetsPanel,
    closeWidgetsPanel,
    handleToggleNotifications,
  };
};
