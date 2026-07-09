import { IconBlock } from '@edifice.io/react/icons';
import clsx from 'clsx';
import emptyFavoritesIllustration from './assets/empty-favorites.svg';

interface MediacentreStateMessageProps {
  variant: 'empty' | 'error';
  text: string;
}

export function MediacentreStateMessage({
  variant,
  text,
}: MediacentreStateMessageProps) {
  return (
    <div
      className={clsx(
        'mediacentre-state-message',
        `mediacentre-state-message--${variant}`,
      )}
    >
      {variant === 'empty' ? (
        <img
          src={emptyFavoritesIllustration}
          alt=""
          className="mediacentre-state-message-illustration"
        />
      ) : (
        <div className="mediacentre-state-message-icon">
          <IconBlock />
        </div>
      )}
      <p className="mediacentre-state-message-text">{text}</p>
    </div>
  );
}
