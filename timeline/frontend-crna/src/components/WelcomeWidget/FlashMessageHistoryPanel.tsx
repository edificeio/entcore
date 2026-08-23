import { EmptyScreen, Heading, IconButton, useOverlay } from '@edifice.io/react';
import { MessageFlash } from '@edifice.io/react/homepage';
import { IconClose } from '@edifice.io/react/icons';
import illuEmptyNotification from '@edifice.io/bootstrap/dist/images/homepage/illu-empty-notifications.png';
import { useTranslation } from 'react-i18next';
import { useFlashMessageHistory } from '~/services/queries/flashMessage.queries';
import { WidgetSkeleton } from '../ui/WidgetSkeleton';
import './FlashMessageHistoryPanel.css';

export function FlashMessageHistoryPanel() {
  const { t } = useTranslation('timeline');
  const { updateOverlayOpen } = useOverlay();
  const closeOverlay = () => updateOverlayOpen(false);
  const { data: messages, isLoading, isError } = useFlashMessageHistory();

  return (
    <div className="flash-message-history-panel">
      <div className="flash-message-history-panel-header">
        <Heading
          level="h2"
          headingStyle="h5"
          className="flash-message-history-panel-title"
        >
          {t('homepage.crna.widget.welcome.history.title', 'Historique des message flash')}
        </Heading>
        <IconButton
          icon={<IconClose />}
          variant="ghost"
          color="tertiary"
          aria-label={t('close', 'Fermer')}
          onClick={closeOverlay}
        />
      </div>

      {isLoading ? (
        <WidgetSkeleton />
      ) : isError || !messages || messages.length === 0 ? (
        <EmptyScreen
          imageSrc={illuEmptyNotification}
          text={t(
            'homepage.crna.widget.welcome.history.empty',
            'Aucun message à afficher',
          )}
        />
      ) : (
        <ul className="flash-message-history-list">
          {messages.map((message) => (
            <li key={message.id}>
              <MessageFlash message={message} />
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
