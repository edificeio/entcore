import {
  Alert,
  LoadingScreen,
  PageLayout,
  useBreakpoint,
  useEdificeClient,
  useOverlay,
} from '@edifice.io/react';
import {
  LastInfosContainer,
  MessageFlashListContainer,
  NotificationListContainer,
  SchoolSpaceContainer,
} from '@edifice.io/react/homepage';
import { useState } from 'react';
import backgroundImage from '~/assets/background.png';
import styles from './Root.module.css';
import { MediacentreWidget, WidgetMasonry } from '~/components';
import { AvantagesWidget } from '~/components/AvantagesWidget/AvantagesWidget';
import { BetaSwitchContainer } from '~/components/BetaSwitch/BetaSwitchContainer';
import { CarnetDeBordWidget } from '~/components/CarnetDeBordWidget';
import { PersonnalisationPanel } from '~/components/ui/PersonnalisationPanel';
import { WidgetErrorBoundary } from '~/components/ui/WidgetErrorBoundary';
import { WelcomeWidget } from '~/components/WelcomeWidget';

/** Check old format URL and redirect if needed */
export const loader = async () => {
  return null;
};

export const Root = () => {
  const { init, user } = useEdificeClient();
  const hasMediacentreWidget = user?.widgets?.some(
    (w) => (w.name as string) === 'mediacentre-widget',
  );
  const hasCarnetDeBord = user?.widgets?.some(
    (w) => w.name === 'carnet-de-bord',
  );
  const { updateOverlayOpen } = useOverlay();
  const openOverlay = () => updateOverlayOpen(true);
  const { lg } = useBreakpoint();
  const [pageErrors, setPageErrors] = useState<string[]>([]);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);
  const [isNotificationsOpen, setIsNotificationsOpen] = useState(false);

  const toggleNotifications = () => setIsNotificationsOpen((prev) => !prev);
  const closeNotifications = () => setIsNotificationsOpen(false);

  const isSidebarOpen = isNotificationsOpen && lg;

  const handleWidgetError = (message: string) => {
    setPageErrors((prev) =>
      prev.includes(message) ? prev : [...prev, message],
    );
  };

  if (!init) return <LoadingScreen position={false} />;

  return (
    <PageLayout
      className={styles.layout}
      variant="fullpage"
      style={{
        backgroundImage: `url(${backgroundImage})`,
        backgroundSize: 'cover',
        backgroundPosition: 'center',
        backgroundRepeat: 'no-repeat',
      }}
      scrollMode="columns"
      noPadding={{ sidebarRight: true }}
    >
      <PageLayout.Header onNotificationsClick={toggleNotifications} />
      <PageLayout.SidebarLeft className="d-grid align-content-start bg-white py-16 gap-16">
        {pageErrors.map((msg) => (
          <Alert
            key={msg}
            type="danger"
            isToast
            position="top-right"
            isDismissible
            autoClose
          >
            {msg}
          </Alert>
        ))}
        {successMessage && (
          <Alert
            type="success"
            isToast
            position="top-right"
            isDismissible
            autoClose
            onClose={() => setSuccessMessage(null)}
          >
            {successMessage}
          </Alert>
        )}
        <SchoolSpaceContainer />

        <WidgetErrorBoundary>
          <LastInfosContainer />
        </WidgetErrorBoundary>
      </PageLayout.SidebarLeft>
      <PageLayout.Content className="d-grid align-content-start py-16 gap-16">
        <BetaSwitchContainer />
        <MessageFlashListContainer />

        <WidgetErrorBoundary>
          <WelcomeWidget
            onCreateDocumentSuccess={setSuccessMessage}
            onOpenSettings={openOverlay}
          />
        </WidgetErrorBoundary>

        <WidgetMasonry>
          {hasMediacentreWidget && <MediacentreWidget />}
          <WidgetErrorBoundary>
            <AvantagesWidget />
          </WidgetErrorBoundary>
          {hasCarnetDeBord && (
            <WidgetErrorBoundary>
              <CarnetDeBordWidget onError={handleWidgetError} />
            </WidgetErrorBoundary>
          )}
        </WidgetMasonry>
      </PageLayout.Content>
      <PageLayout.Overlay backdrop closeButton={false}>
        <PersonnalisationPanel />
      </PageLayout.Overlay>

      {isSidebarOpen ? (
        <PageLayout.SidebarRight>
          <NotificationListContainer
            onCloseNotifications={closeNotifications}
          />
        </PageLayout.SidebarRight>
      ) : (
        isNotificationsOpen && (
          <PageLayout.Overlay
            closeButton={true}
            onClose={closeNotifications}
            backdrop={true}
          >
            <NotificationListContainer />
          </PageLayout.Overlay>
        )
      )}
    </PageLayout>
  );
};

export default Root;
