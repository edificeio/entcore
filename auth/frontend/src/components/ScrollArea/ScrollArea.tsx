import {
  type ReactNode,
  useCallback,
  useEffect,
  useRef,
  useState,
} from 'react';
import './ScrollArea.css';

interface ScrollAreaProps {
  children: ReactNode;
}

// Tolerance (px) absorbing sub-pixel rounding of scroll metrics.
const EDGE_TOLERANCE = 1;

/**
 * Vertical scroll container that overlays a shadow on the top/bottom edge
 * while content remains hidden in that direction, so a clipped item is
 * noticeable even when the scrollbar is hidden (overlay scrollbars).
 */
export const ScrollArea = ({ children }: ScrollAreaProps) => {
  const viewportRef = useRef<HTMLDivElement>(null);
  const [hasMoreAbove, setHasMoreAbove] = useState(false);
  const [hasMoreBelow, setHasMoreBelow] = useState(false);

  const updateShadows = useCallback(() => {
    const viewport = viewportRef.current;
    if (!viewport) return;
    const { scrollTop, clientHeight, scrollHeight } = viewport;
    setHasMoreAbove(scrollTop > EDGE_TOLERANCE);
    setHasMoreBelow(scrollTop + clientHeight < scrollHeight - EDGE_TOLERANCE);
  }, []);

  useEffect(() => {
    const viewport = viewportRef.current;
    if (!viewport) return;
    updateShadows();
    if (typeof ResizeObserver === 'undefined') return;
    // Recompute when the viewport (window resize) or its content changes size.
    const observer = new ResizeObserver(updateShadows);
    observer.observe(viewport);
    if (viewport.firstElementChild)
      observer.observe(viewport.firstElementChild);
    return () => observer.disconnect();
  }, [updateShadows]);

  return (
    <div className="wayf-scroll-area">
      <div
        ref={viewportRef}
        className="wayf-scroll-area__viewport"
        onScroll={updateShadows}
      >
        {children}
      </div>
      <span
        aria-hidden="true"
        className="wayf-scroll-area__shadow wayf-scroll-area__shadow--top"
        data-visible={hasMoreAbove}
      />
      <span
        aria-hidden="true"
        className="wayf-scroll-area__shadow wayf-scroll-area__shadow--bottom"
        data-visible={hasMoreBelow}
      />
    </div>
  );
};
