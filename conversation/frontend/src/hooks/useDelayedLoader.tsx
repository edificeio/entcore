import { useEffect, useRef, useState } from 'react';

export function useDelayedLoader(isLoading: boolean, delay = 400) {
  const [showLoader, setShowLoader] = useState(false);
  const timeoutRef = useRef<NodeJS.Timeout | null>(null);

  useEffect(() => {
    if (timeoutRef.current) {
      clearTimeout(timeoutRef.current);
      timeoutRef.current = null;
    }

    if (isLoading && delay > 0) {
      timeoutRef.current = setTimeout(() => {
        setShowLoader(true);
      }, delay);
    } else {
      setShowLoader(false);
    }

    return () => {
      if (timeoutRef.current) {
        clearTimeout(timeoutRef.current);
        timeoutRef.current = null;
      }
    };
  }, [isLoading, delay]);

  return showLoader;
}
