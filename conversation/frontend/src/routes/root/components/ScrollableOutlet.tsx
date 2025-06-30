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

  const saveScroll = () => {
    if (scrollRef.current) {
      setCurrentScrollPosition(scrollRef.current.scrollTop);
    }
  };
  useEffect(() => {
    const scrollElement = scrollRef.current;
    if (scrollElement) {
      scrollElement.addEventListener('scroll', saveScroll);
      return () => {
        scrollElement.removeEventListener('scroll', saveScroll);
      };
    }
  }, []);

  const restoreScroll = () => {
    if (scrollRef.current) {
      scrollRef.current.scrollTo({
        top: state?.savedScrollPosition || 0,
      });
    }
  };
  useEffect(() => {
    if (folderId && !isPending) {
      restoreScroll();
    }
  }, [folderId, isPending, state?.savedScrollPosition]);

  return (
    <div
      className="flex-fill overflow-y-auto position-relative pb-64"
      ref={scrollRef}
    >
      <Outlet />
    </div>
  );
}
