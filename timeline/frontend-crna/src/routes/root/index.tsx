import {
  LoadingScreen,
  PageLayout,
  useBreakpoint,
  useEdificeClient,
  useOverlay,
} from '@edifice.io/react';
import {
  AgendaContainer,
  AlloRegion,
  BriefMeContainer,
  CantineContainer,
  CommunitiesContainer,
  EdumalinContainer,
  GenerationHdfContainer,
  LastInfosContainer,
  MessageFlashListContainer,
  NotificationListContainer,
  QwantContainer,
  RssContainer,
  SchoolSpaceContainer,
  ScreenTimeContainer,
  UniversalisContainer,
  UsefulLinksContainer,
} from '@edifice.io/react/homepage';
import { useEffect, useState } from 'react';
import styles from './Root.module.css';
import { MediacentreWidget, WidgetMasonry } from '~/components';
import { AvantagesWidget } from '~/components/AvantagesWidget/AvantagesWidget';
import { CarnetDeBordWidget } from '~/components/CarnetDeBordWidget';
import { WidgetErrorBoundary } from '~/components/ui/WidgetErrorBoundary';
import { TimetableWidget } from '~/components/TimetableWidget';
import { FlashMessageHistoryPanel } from '~/components/WelcomeWidget/FlashMessageHistoryPanel';
import { WelcomeWidget } from '~/components/WelcomeWidget';
import { WidgetsSettingsPanel } from '~/components/WidgetsSettingsPanel';
import { useAvailableWidgets } from '~/hooks/useAvailableWidgets';
import { useWidgetPreferences } from '~/hooks/useWidgetPreferences';
import type { WidgetId } from '~/models/widgetPreferences';

/** Rendered without AlloRegionContainer, which hides the card without the alloregion right. */
const ALLO_REGION_ADDRESS = '/alloregion';

type OverlayPanel =
  | 'flash-history'
  | 'notifications'
  | 'widgets-settings'
  | null;

/** Check old format URL and redirect if needed */
export const loader = async () => {
  return null;
};

export const Root = () => {
  const { init } = useEdificeClient();
  const available = useAvailableWidgets();
  const { isVisible } = useWidgetPreferences();
  /** A widget is shown when the user can access it and hasn't hidden it. */
  const show = (id: WidgetId) => available[id] && isVisible(id);
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

  const openHistory = () => {
    setActivePanel('flash-history');
    updateOverlayOpen(true);
  };
  const openWidgetsSettings = () => {
    setActivePanel('widgets-settings');
    updateOverlayOpen(true);
  };
  const closeWidgetsSettings = () => {
    setActivePanel(null);
    updateOverlayOpen(false);
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
        <MessageFlashListContainer />

        <WidgetErrorBoundary>
          <WelcomeWidget
            onOpenHistory={openHistory}
            onOpenSettings={openWidgetsSettings}
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

        {show('communities') && (
          <WidgetErrorBoundary>
            <CommunitiesContainer
              onCommunityClick={(community) => openCommunity(community.id)}
              onHeaderActionClick={openCommunities}
            />
          </WidgetErrorBoundary>
        )}

        <WidgetMasonry>
          {show('mediacentre') && <MediacentreWidget />}
          {show('avantages') && (
            <WidgetErrorBoundary>
              <AvantagesWidget />
            </WidgetErrorBoundary>
          )}
          {show('carnet-de-bord') && (
            <WidgetErrorBoundary>
              <CarnetDeBordWidget onError={handleWidgetError} />
            </WidgetErrorBoundary>
          )}
          {show('useful-links') && (
            <WidgetErrorBoundary>
              <UsefulLinksContainer />
            </WidgetErrorBoundary>
          )}
          {show('agenda') && (
            <WidgetErrorBoundary>
              <AgendaContainer />
            </WidgetErrorBoundary>
          )}
          {/* TODO: gate behind a user.widgets flag once the timetable/EDT backend API exists — mock data for now. */}
          {show('timetable') && (
            <WidgetErrorBoundary>
              <TimetableWidget />
            </WidgetErrorBoundary>
          )}
          {show('cantine') && (
            <WidgetErrorBoundary>
              <CantineContainer />
            </WidgetErrorBoundary>
          )}
          {show('screen-time') && (
            <WidgetErrorBoundary>
              <ScreenTimeContainer />
            </WidgetErrorBoundary>
          )}
          {show('edumalin') && (
            <WidgetErrorBoundary>
              <EdumalinContainer />
            </WidgetErrorBoundary>
          )}
          {show('briefme') && (
            <WidgetErrorBoundary>
              <BriefMeContainer />
            </WidgetErrorBoundary>
          )}
          {show('rss') && (
            <WidgetErrorBoundary>
              <RssContainer />
            </WidgetErrorBoundary>
          )}
          {show('universalis') && (
            <WidgetErrorBoundary>
              <UniversalisContainer />
            </WidgetErrorBoundary>
          )}
          {show('qwant') && (
            <WidgetErrorBoundary>
              <QwantContainer />
            </WidgetErrorBoundary>
          )}
          {show('generation-hdf') && (
            <WidgetErrorBoundary>
              <GenerationHdfContainer />
            </WidgetErrorBoundary>
          )}
          {show('allo-region') && (
            <WidgetErrorBoundary>
              <AlloRegion
                onRequestsClick={() =>
                  window.open(ALLO_REGION_ADDRESS, '_self')
                }
                onNewRequestClick={() =>
                  window.open(`${ALLO_REGION_ADDRESS}/new`, '_self')
                }
              />
            </WidgetErrorBoundary>
          )}
        </WidgetMasonry>
      </PageLayout.Content>
      <PageLayout.Overlay
        backdrop
        closeButton={false}
        onClose={isNotificationsOpen ? closeNotifications : undefined}
      >
        {activePanel === 'flash-history' && <FlashMessageHistoryPanel />}
        {activePanel === 'widgets-settings' && (
          <WidgetsSettingsPanel onClose={closeWidgetsSettings} />
        )}
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
