import { IUserInfo } from '@edifice.io/client';
import { useScreeb } from '@screeb/sdk-react';
import { useCallback, useRef } from 'react';

export function useScreebIdentity() {
  const { identity } = useScreeb();
  const sentUserIdRef = useRef<string | null>(null);

  const setIdentity = useCallback(
    async (user: IUserInfo) => {
      // Skip call if the userId has already been sent to Screeb
      if (sentUserIdRef.current === user.userId) {
        return;
      }

      try {
        const hashBuffer = await crypto.subtle.digest(
          'SHA-256',
          new TextEncoder().encode(user.userId),
        );

        const hashArray = Array.from(new Uint8Array(hashBuffer));
        const hashedUserId = hashArray
          .map((b) => b.toString(16).padStart(2, '0'))
          .join('')
          .slice(0, 16); // 16 chars stable

        identity(hashedUserId, {
          profile: user.type,
        });
        sentUserIdRef.current = user.userId;
      } catch (error) {
        console.error('Failed to send identity to Screeb', error);
      }
    },
    [identity],
  );

  return { setIdentity };
}
