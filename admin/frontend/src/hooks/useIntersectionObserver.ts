import { useCallback, useEffect, useRef } from 'react';

/**
 * Calls `onIntersect` each time the referenced element enters the viewport.
 * Returns a ref callback to attach to the observed element.
 */
export function useIntersectionObserver(
  onIntersect: () => void,
  { enabled = true, rootMargin = '200px' } = {},
) {
  const observerRef = useRef<IntersectionObserver | null>(null);
  const callbackRef = useRef(onIntersect);

  useEffect(() => {
    callbackRef.current = onIntersect;
  }, [onIntersect]);

  useEffect(() => () => observerRef.current?.disconnect(), []);

  return useCallback(
    (element: Element | null) => {
      observerRef.current?.disconnect();
      if (!element || !enabled || typeof IntersectionObserver === 'undefined')
        return;

      observerRef.current = new IntersectionObserver(
        (entries) => {
          if (entries.some((entry) => entry.isIntersecting))
            callbackRef.current();
        },
        { rootMargin },
      );
      observerRef.current.observe(element);
    },
    [enabled, rootMargin],
  );
}
