import DOMPurify from 'dompurify';
import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import type { WelcomeResponse, WelcomeState } from '~/models/welcome';

const TIMEOUT_MS = 5000;

function pickContent(response: WelcomeResponse, lang: string): string | null {
  if (!response.enabled) return null;
  const candidate = response[lang];
  if (typeof candidate === 'string' && candidate.trim().length > 0) return candidate;
  const fallback = response.fr;
  if (typeof fallback === 'string' && fallback.trim().length > 0) return fallback;
  return null;
}

export function useWelcomeMessage(): WelcomeState {
  const { i18n } = useTranslation();
  const [state, setState] = useState<WelcomeState>({ status: 'loading' });

  useEffect(() => {
    const controller = new AbortController();
    const timeoutId = setTimeout(() => controller.abort(), TIMEOUT_MS);

    (async () => {
      try {
        const res = await fetch(
          '/auth/configure/welcome?allLanguages=allLanguages',
          { credentials: 'include', signal: controller.signal },
        );

        if (!res.ok) {
          setState({ status: 'hidden' });
          return;
        }

        const data: WelcomeResponse = await res.json();
        const content = pickContent(data, i18n.language);

        if (!content) {
          setState({ status: 'hidden' });
          return;
        }

        setState({
          status: 'ready',
          html: DOMPurify.sanitize(content, {
            ADD_TAGS: ['iframe'],
            ADD_ATTR: ['allowfullscreen', 'frameborder', 'src', 'width', 'height'],
          }),
        });
      } catch (err) {
        if ((err as Error).name !== 'AbortError') {
          console.warn('[WAYF] Failed to load welcome message:', err);
        }
        setState({ status: 'hidden' });
      } finally {
        clearTimeout(timeoutId);
      }
    })();

    return () => {
      controller.abort();
      clearTimeout(timeoutId);
    };
  }, [i18n.language]);

  return state;
}

export { pickContent };
