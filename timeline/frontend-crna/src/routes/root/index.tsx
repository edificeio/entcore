import {
  Alert,
  LoadingScreen,
  PageLayout,
  useBreakpoint,
  useEdificeClient,
  useHasWorkflow,
  useOverlay,
} from '@edifice.io/react';
import {
  CommunitiesContainer,
  LastInfosContainer,
  MessageFlashListContainer,
  NotificationListContainer,
  SchoolSpaceContainer,
} from '@edifice.io/react/homepage';
import { useEffect, useState } from 'react';
import backgroundImage from '~/assets/background.png';
import styles from './Root.module.css';
import { MediacentreWidget, WidgetMasonry } from '~/components';
import { AgendaWidget } from '~/components/AgendaWidget';
import { AvantagesWidget } from '~/components/AvantagesWidget/AvantagesWidget';
import { CarnetDeBordWidget } from '~/components/CarnetDeBordWidget';
import { PersonnalisationPanel } from '~/components/ui/PersonnalisationPanel';
import { WidgetErrorBoundary } from '~/components/ui/WidgetErrorBoundary';
import { TimetableWidget } from '~/components/TimetableWidget';
import { FlashMessageHistoryPanel } from '~/components/WelcomeWidget/FlashMessageHistoryPanel';
import { WelcomeWidget } from '~/components/WelcomeWidget';
import { useWidgetPreferences } from '~/hooks/useWidgetPreferences';

type OverlayPanel = 'settings' | 'flash-history' | 'notifications' | null;

/** Check old format URL and redirect if needed */
export const loader = async () => {
  return null;
};

export const Root = () => {
  const { init, user } = useEdificeClient();
  const hasMediacentreWidget =
    user?.widgets?.some((w) => (w.name as string) === 'mediacentre-widget') ||
    useHasWorkflow(
      'fr.openent.mediacentre.controller.MediacentreController|render',
    );
  const hasCarnetDeBord = user?.widgets?.some(
    (w) => w.name === 'carnet-de-bord',
  );
  const hasAgendaWidget = user?.widgets?.some(
    (w) => (w.name as string) === 'agenda-widget',
  );
  const { isVisible } = useWidgetPreferences();
  const { isOverlayOpen, updateOverlayOpen } = useOverlay();
  const [activePanel, setActivePanel] = useState<OverlayPanel>(null);
  const { md, lg } = useBreakpoint();
  // Below `md` (768px), PageLayout stacks into a single column — drop the
  // SidebarLeft split and reflow everything into Content in the requested
  // order instead of the default breadcrumb → sidebarLeft → content order.
  const isMobile = !md;

  const isNotificationsOpen = activePanel === 'notifications';
  // On desktop, notifications live in the SidebarRight (grid column) instead
  // of the slide-in overlay, so they don't need `overlayOpen` to be true.
  const isSidebarOpen = isNotificationsOpen && lg;

  const openSettings = () => {
    setActivePanel('settings');
    updateOverlayOpen(true);
  };
  const openHistory = () => {
    setActivePanel('flash-history');
    updateOverlayOpen(true);
  };
  const openNotifications = () => {
    setActivePanel('notifications');
    updateOverlayOpen(!lg);
  };
  const closeNotifications = () => {
    setActivePanel(null);
    updateOverlayOpen(false);
  };
  const toggleNotifications = () =>
    isNotificationsOpen ? closeNotifications() : openNotifications();

  const openCommunity = (communityId: string | number) =>
    window.open(`/community#/view/${communityId}`, '_self');
  const openCommunities = () => window.open('/community', '_self');

  useEffect(() => {
    if (isOverlayOpen) return;
    // Switching from overlay to SidebarRight closes the overlay without
    // actually dismissing the notifications panel — keep it active.
    if (activePanel === 'notifications' && lg) return;
    setActivePanel(null);
  }, [isOverlayOpen, activePanel, lg]);

  // Keep the shared overlay state in sync when resizing while notifications
  // are open, so it opens/closes the slide-in panel as the layout switches
  // between SidebarRight (desktop) and Overlay (mobile/tablet).
  useEffect(() => {
    if (activePanel === 'notifications') updateOverlayOpen(!lg);
  }, [lg, activePanel, updateOverlayOpen]);

  const [pageErrors, setPageErrors] = useState<string[]>([]);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);

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
      {!isMobile && (
        <PageLayout.SidebarLeft className="d-grid align-content-start bg-white py-16 gap-16">
          <SchoolSpaceContainer />

          <WidgetErrorBoundary>
            <LastInfosContainer />
          </WidgetErrorBoundary>
        </PageLayout.SidebarLeft>
      )}
      <PageLayout.Content className="d-grid align-content-start py-16 gap-16">
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
        <MessageFlashListContainer />

        <WidgetErrorBoundary>
          <WelcomeWidget
            onCreateDocumentSuccess={setSuccessMessage}
            onOpenSettings={openSettings}
            onOpenHistory={openHistory}
          />
        </WidgetErrorBoundary>

        {isMobile && (
          <>
            <SchoolSpaceContainer />

            <WidgetErrorBoundary>
              <LastInfosContainer />
            </WidgetErrorBoundary>
          </>
        )}

        {isVisible('communities') && (
          <WidgetErrorBoundary>
            <CommunitiesContainer
              onCommunityClick={(community) => openCommunity(community.id)}
              onHeaderActionClick={openCommunities}
            />
          </WidgetErrorBoundary>
        )}

        <WidgetMasonry>
          {hasMediacentreWidget && isVisible('mediacentre') && (
            <MediacentreWidget />
          )}
          {isVisible('avantages') && (
            <WidgetErrorBoundary>
              <AvantagesWidget />
            </WidgetErrorBoundary>
          )}
          {hasCarnetDeBord && isVisible('carnet-de-bord') && (
            <WidgetErrorBoundary>
              <CarnetDeBordWidget onError={handleWidgetError} />
            </WidgetErrorBoundary>
          )}
          {hasAgendaWidget && isVisible('agenda') && (
            <WidgetErrorBoundary>
              <AgendaWidget />
            </WidgetErrorBoundary>
          )}
          {/* TODO: gate behind a user.widgets flag once the timetable/EDT backend API exists — mock data for now. */}
          {isVisible('timetable') && (
            <WidgetErrorBoundary>
              <TimetableWidget />
            </WidgetErrorBoundary>
          )}
        </WidgetMasonry>
      </PageLayout.Content>
      <PageLayout.Overlay
        backdrop
        closeButton={false}
        onClose={isNotificationsOpen ? closeNotifications : undefined}
      >
        {activePanel === 'settings' && <PersonnalisationPanel />}
        {activePanel === 'flash-history' && <FlashMessageHistoryPanel />}
        {isNotificationsOpen && !isSidebarOpen && (
          <NotificationListContainer
            onCloseNotifications={closeNotifications}
          />
        )}
      </PageLayout.Overlay>

      {isSidebarOpen && (
        <PageLayout.SidebarRight>
          <NotificationListContainer
            onCloseNotifications={closeNotifications}
          />
        </PageLayout.SidebarRight>
      )}
    </PageLayout>
  );
};

export default Root;
