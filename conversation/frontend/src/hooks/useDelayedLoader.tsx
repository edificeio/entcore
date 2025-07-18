import { useEffect, useState } from 'react';

export function useDelayedLoader(isLoading: boolean, delay = 300) {
  const [showLoader, setShowLoader] = useState(false);

  useEffect(() => {
    let timeout: NodeJS.Timeout | undefined;

    if (isLoading) {
      timeout = setTimeout(() => {
        setShowLoader(true);
      }, delay);
    } else {
      clearTimeout(timeout);
      setShowLoader(false);
    }

    return () => clearTimeout(timeout);
  }, [isLoading, delay]);

  return showLoader;
}
