import {
  LoadingScreen,
  PageLayout,
  useBreakpoint,
  useEdificeClient,
  useOverlay,
} from '@edifice.io/react';
import {
  FavoritesContainer,
  LastInfosContainer,
  MessageFlashListContainer,
  NotificationListContainer,
  SchoolSpaceContainer,
  UsefulLinksContainer,
  UserSpaceContainer,
} from '@edifice.io/react/homepage';
import { useState } from 'react';
import { BetaSwitchContainer } from '~/components/BetaSwitch/BetaSwitchContainer';
import { WidgetsPersonalizationPanelContainer } from '~/components/WidgetsPersonalizationPanel/WidgetsPersonalizationPanelContainer';
import { useNotificationsLayout } from './hooks/useNotificationsLayout';

/** Check old format URL and redirect if needed */
export const loader = async () => {
  return null;
};

export const Root = () => {
  const { init } = useEdificeClient();
  const { isSidebarOpen, toggleNotifications, closeNotifications } =
    useNotificationsLayout();
  const { md } = useBreakpoint();
  const { updateOverlayOpen } = useOverlay();
  const [isWidgetsPanelOpen, setIsWidgetsPanelOpen] = useState(false);

  // The overlay only has one content slot, shared with notifications — keep
  // the two mutually exclusive rather than stacking them.
  const openWidgetsPanel = () => {
    closeNotifications();
    setIsWidgetsPanelOpen(true);
    updateOverlayOpen(true);
  };
  const closeWidgetsPanel = () => {
    setIsWidgetsPanelOpen(false);
    updateOverlayOpen(false);
  };
  const handleToggleNotifications = () => {
    setIsWidgetsPanelOpen(false);
    toggleNotifications();
  };

  if (!init) return <LoadingScreen position={false} />;

  return (
    <PageLayout
      scrollMode="columns"
      variant="fullpage"
      noPadding={{
        sidebarRight: true,
      }}
    >
      <PageLayout.Header onNotificationsClick={handleToggleNotifications} />
      <PageLayout.SidebarLeft className="bg-white">
        <div className="d-flex flex-column py-16 gap-16 ">
          {!md && <MessageFlashListContainer />}

          <SchoolSpaceContainer />
          <LastInfosContainer />
        </div>
      </PageLayout.SidebarLeft>
      <PageLayout.Content>
        <div className="d-flex flex-column py-16 gap-16">
          <BetaSwitchContainer />
          {md && <MessageFlashListContainer />}
          <UserSpaceContainer onCustomizeWidgetsClick={openWidgetsPanel}>
            <FavoritesContainer />
          </UserSpaceContainer>
          {/* TODO: gate on isVisible('<widget-name>') once "Liens utiles"
              is registered as a widget in the catalog */}
          <UsefulLinksContainer />
        </div>
      </PageLayout.Content>

      {isSidebarOpen ? (
        <PageLayout.SidebarRight>
          <NotificationListContainer
            onCloseNotifications={closeNotifications}
          />
        </PageLayout.SidebarRight>
      ) : (
        <PageLayout.Overlay
          closeButton={!isWidgetsPanelOpen}
          onClose={isWidgetsPanelOpen ? closeWidgetsPanel : closeNotifications}
          backdrop={true}
        >
          {isWidgetsPanelOpen ? (
            <WidgetsPersonalizationPanelContainer onClose={closeWidgetsPanel} />
          ) : (
            <NotificationListContainer />
          )}
        </PageLayout.Overlay>
      )}
      <PageLayout.HelpZone />
    </PageLayout>
  );
};

export default Root;
