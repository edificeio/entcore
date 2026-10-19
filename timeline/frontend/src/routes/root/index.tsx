import {
  LoadingScreen,
  Masonry,
  PageLayout,
  useBreakpoint,
  useEdificeClient,
} from '@edifice.io/react';
import {
  FavoritesContainer,
  LastInfosContainer,
  MessageFlashListContainer,
  NotificationListContainer,
  SchoolSpaceContainer,
  UserSpaceContainer,
} from '@edifice.io/react/homepage';
import { useState } from 'react';
import { BetaSwitchContainer } from '~/components/BetaSwitch/BetaSwitchContainer';
import { WidgetsPersonalizationPanelContainer } from '~/components/WidgetsPersonalizationPanel/WidgetsPersonalizationPanelContainer';
import { useHomepageWidgets } from '~/hooks/useHomepageWidgets';
import { useNotificationsLayout } from './hooks/useNotificationsLayout';
import { useWidgetsPanelLayout } from './hooks/useWidgetsPanelLayout';

/** Check old format URL and redirect if needed */
export const loader = async () => {
  return null;
};

export const Root = () => {
  const { init } = useEdificeClient();
  const { displayed: displayedWidgets } = useHomepageWidgets();
  // Lifted here because both layout hooks depend on it (overlay sync lives in
  // useNotificationsLayout)
  const [isWidgetsPanelOpen, setIsWidgetsPanelOpen] = useState(false);
  const { isSidebarOpen, toggleNotifications, closeNotifications } =
    useNotificationsLayout({ isWidgetsPanelOpen });
  const { md } = useBreakpoint();
  const columns = md ? 2 : 1;
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
          <Masonry columns={columns} gap="16">
            {displayedWidgets.map(({ name, fullWidth, Component }) => (
              <Masonry.Item key={name} span={fullWidth ? columns : 1}>
                <Component />
              </Masonry.Item>
            ))}
          </Masonry>
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
