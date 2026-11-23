import { useCallback, useEffect, useRef } from 'react';
import { BlockerFunction, useBlocker } from 'react-router-dom';

const warnBeforeUnload = (event: BeforeUnloadEvent) => {
  event.preventDefault();
  // Chromium before 119 only shows the prompt when returnValue is set.
  event.returnValue = '';
};

/**
 * Guard against leaving the page with unsaved changes.
 * - In-app navigations (links, browser back/forward inside the SPA) are blocked
 *   through the returned `blocker`, so the caller can show its own modal.
 * - Document unloads (full page links, closing the tab, leaving the SPA history)
 *   trigger the browser's native confirmation through `beforeunload`.
 *
 * Call `bypass()` right before a navigation the user already confirmed.
 */
export function useUnsavedChangesGuard(when: boolean) {
  const bypassRef = useRef(false);

  const shouldBlock = useCallback<BlockerFunction>(
    ({ currentLocation, nextLocation }) =>
      when &&
      !bypassRef.current &&
      currentLocation.pathname !== nextLocation.pathname,
    [when],
  );
  const blocker = useBlocker(shouldBlock);

  useEffect(() => {
    if (!when) return;
    const handleBeforeUnload = (event: BeforeUnloadEvent) => {
      if (!bypassRef.current) warnBeforeUnload(event);
    };
    window.addEventListener('beforeunload', handleBeforeUnload);
    return () => window.removeEventListener('beforeunload', handleBeforeUnload);
  }, [when]);

  // Release a pending blocked navigation once there is nothing left to protect.
  useEffect(() => {
    if (!when && blocker.state === 'blocked') blocker.reset();
  }, [when, blocker]);

  const bypass = useCallback(() => {
    bypassRef.current = true;
  }, []);

  return { blocker, bypass };
}
