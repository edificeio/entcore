import { useEffect, useRef } from 'react';
import { Outlet, useLocation } from 'react-router-dom';
import { useSelectedFolder } from '~/hooks/useSelectedFolder';
import { useFolderMessages } from '~/services/queries';
import { useScrollStore } from '~/store/scrollStore';

export function ScrollableOutlet() {
  const { folderId } = useSelectedFolder();
  const { isPending } = useFolderMessages(folderId!);
  const { state } = useLocation();
  const scrollRef = useRef<HTMLDivElement>(null);
  const setCurrentScrollPosition =
    useScrollStore.use.setCurrentScrollPosition();
  const savedScrollPosition = useScrollStore.use.savedScrollPosition();

  useEffect(() => {
    const scrollElement = scrollRef.current;
    if (scrollElement) {
      scrollElement.addEventListener('scroll', saveScroll);
      return () => {
        scrollElement.removeEventListener('scroll', saveScroll);
      };
    }
  }, []);

  useEffect(() => {
    if (folderId && !isPending) {
      restoreScroll(state?.scrollPositionToRestore || 0);
    }
  }, [folderId, isPending, state?.scrollPositionToRestore]);

  useEffect(() => {
    const handlePopState = () => {
      restoreScroll(savedScrollPosition);
    };
    window.addEventListener('popstate', handlePopState);
    return () => {
      window.removeEventListener('popstate', handlePopState);
    };
  }, [savedScrollPosition]);

  const saveScroll = () => {
    if (scrollRef.current) {
      setCurrentScrollPosition(scrollRef.current.scrollTop);
    }
  };

  const restoreScroll = (scrollPosition: number) => {
    scrollRef.current?.scrollTo({
      top: scrollPosition || 0,
    });
  };

  return (
    <div
      className="flex-fill overflow-y-auto position-relative pb-64"
      ref={scrollRef}
    >
      <Outlet />
    </div>
  );
}
