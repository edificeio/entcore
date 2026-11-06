import { useEdificeClient } from '@edifice.io/react';
import { useEffect } from 'react';

/**
 * Set a relevant page title (RGAA 8.5 / 8.6).
 *
 * EdificeClientProvider resets `document.title` to the app name once the session is loaded,
 * in an effect that runs after its children's ones: the title is applied on the next tick
 * and re-applied each time the provider may have overwritten it.
 */
export function usePageTitle(title: string) {
  const { init } = useEdificeClient();

  useEffect(() => {
    const timeout = setTimeout(() => {
      document.title = title;
    });
    return () => clearTimeout(timeout);
  }, [title, init]);
}
