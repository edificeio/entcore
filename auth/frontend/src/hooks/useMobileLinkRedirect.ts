import { useEffect } from 'react';

interface ReactNativeWebView {
  postMessage: (message: string) => void;
}

declare global {
  interface Window {
    ReactNativeWebView?: ReactNativeWebView;
  }
}

/** Reads the `mobile` flag injected by the backend in the `saml-wayf` script. */
function isMobileContext(): boolean {
  try {
    const raw = document.getElementById('saml-wayf')?.textContent ?? '{}';
    // The backend injects "true"/"false" as a string in the bootstrap script.
    return JSON.parse(raw).mobile === 'true';
  } catch {
    return false;
  }
}

/**
 * In the mobile app webview, every link that is NOT a provider connection
 * button — the help link, the Edifice badge, the charter link and any link
 * inside the editorial welcome message — must be handed back to the native app
 * instead of opening inside the webview. The native side then opens it in the
 * responsive browser. (ENABLING-901)
 *
 * Provider connection links are plain buttons that navigate via
 * `window.location`, not `<a>` elements, so they are left untouched and keep
 * opening in the webview as expected.
 *
 * A capture-phase listener on the document is used so it also catches links
 * rendered through `dangerouslySetInnerHTML` (the welcome message).
 */
export function useMobileLinkRedirect(): void {
  useEffect(() => {
    if (!isMobileContext()) return;

    const onClick = (event: MouseEvent) => {
      // Only act inside the native app, where the bridge is injected; in a
      // plain mobile browser links must keep working normally.
      if (!window.ReactNativeWebView) return;

      const target = event.target as HTMLElement | null;
      const anchor = target?.closest('a[href]') as HTMLAnchorElement | null;
      if (!anchor) return;

      const href = anchor.getAttribute('href');
      if (!href || href.startsWith('#')) return;

      event.preventDefault();
      window.ReactNativeWebView.postMessage(
        JSON.stringify({ type: 'REDIRECT', url: anchor.href }),
      );
    };

    document.addEventListener('click', onClick, true);
    return () => document.removeEventListener('click', onClick, true);
  }, []);
}
