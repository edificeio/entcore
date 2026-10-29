import { useEffect } from 'react';
import { useFavoritesReorderStore } from '~/store/favoritesReorderStore';

const warnBeforeUnload = (event: BeforeUnloadEvent) => {
  event.preventDefault();
  // Chromium before 119 only shows the prompt when returnValue is set.
  event.returnValue = '';
};

export const useUnsavedOrderWarning = (hasUnsavedOrder: boolean) => {
  const setHasUnsavedOrder = useFavoritesReorderStore(
    (state) => state.setHasUnsavedOrder,
  );

  useEffect(() => {
    setHasUnsavedOrder(hasUnsavedOrder);
    if (!hasUnsavedOrder) return;

    window.addEventListener('beforeunload', warnBeforeUnload);
    return () => {
      window.removeEventListener('beforeunload', warnBeforeUnload);
      setHasUnsavedOrder(false);
    };
  }, [hasUnsavedOrder, setHasUnsavedOrder]);
};
