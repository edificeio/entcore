import {
  LoadingScreen,
  PageLayout,
  useBreakpoint,
  useEdificeClient,
} from '@edifice.io/react';
import {
  AgendaContainer,
  CommunitiesContainer,
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
import { useWidgetPreferences } from '~/hooks/useWidgetPreferences';
import { useNotificationsLayout } from './hooks/useNotificationsLayout';
import { useWidgetsPanelLayout } from './hooks/useWidgetsPanelLayout';

/** Check old format URL and redirect if needed */
export const loader = async () => {
  return null;
};

export const Root = () => {
  const { init } = useEdificeClient();
  const { isVisible } = useWidgetPreferences();
  // Lifted here because both layout hooks depend on it (overlay sync lives in
  // useNotificationsLayout)
  const [isWidgetsPanelOpen, setIsWidgetsPanelOpen] = useState(false);
  const { isSidebarOpen, toggleNotifications, closeNotifications } =
    useNotificationsLayout({ isWidgetsPanelOpen });
  const { md } = useBreakpoint();
  const {
    isWidgetsPanelMounted,
    openWidgetsPanel,
    closeWidgetsPanel,
    handleToggleNotifications,
  } = useWidgetsPanelLayout({
    isWidgetsPanelOpen,
    setIsWidgetsPanelOpen,
    toggleNotifications,
    closeNotifications,
  });

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
          <CommunitiesContainer />
          {isVisible('agenda-widget') && <AgendaContainer />}
          {/* TODO: gate on isVisible('<widget-name>') once "Liens utiles"
              is registered as a widget in the catalog */}
          <UsefulLinksContainer />
        </div>
      </PageLayout.Content>

      {isSidebarOpen && (
        <PageLayout.SidebarRight>
          <NotificationListContainer
            onCloseNotifications={closeNotifications}
          />
        </PageLayout.SidebarRight>
      )}
      {/* Always mounted (even while the notifications sidebar is shown) so the
          slide-in transition plays and it can stack over the sidebar. */}
      <PageLayout.Overlay
        closeButton={!isWidgetsPanelOpen}
        onClose={isWidgetsPanelOpen ? closeWidgetsPanel : closeNotifications}
        backdrop={true}
      >
        {isWidgetsPanelMounted ? (
          <WidgetsPersonalizationPanelContainer onClose={closeWidgetsPanel} />
        ) : (
          // On desktop notifications live in the sidebar, not in the overlay
          !md && <NotificationListContainer />
        )}
      </PageLayout.Overlay>
      <PageLayout.HelpZone />
    </PageLayout>
  );
};

export default Root;
