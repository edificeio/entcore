import { useEffect, useRef } from 'react';
import { Outlet, useLocation } from 'react-router-dom';
import { useSelectedFolder } from '~/hooks/useSelectedFolder';
import { useFolderMessages } from '~/services/queries';
import { useScrollStore } from '~/store/scrollStore';

export function ScrollableOutlet() {
  const { folderId } = useSelectedFolder();
  const { isPending } = useFolderMessages(folderId!);
  const scrollRef = useRef<HTMLDivElement>(null);
  const { state } = useLocation();
  const setCurrentScrollPosition =
    useScrollStore.use.setCurrentScrollPosition();
  const savedScrollPosition = useScrollStore.use.savedScrollPosition();

  useEffect(() => {
    const handlePopState = () => {
      restoreScroll(savedScrollPosition);
    };
    window.addEventListener('popstate', handlePopState);
    return () => {
      window.removeEventListener('popstate', handlePopState);
    };
  }, [savedScrollPosition]);

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
      restoreScroll(state?.savedScrollPosition || 0);
    }
  }, [folderId, isPending, state?.savedScrollPosition]);

  const restoreScroll = (scrollPosition: number) => {
    scrollRef.current?.scrollTo({
      top: scrollPosition || 0,
    });
  };

  const saveScroll = () => {
    if (scrollRef.current) {
      setCurrentScrollPosition(scrollRef.current.scrollTop);
    }
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
