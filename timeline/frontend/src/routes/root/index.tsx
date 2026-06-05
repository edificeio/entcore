import { LoadingScreen, PageLayout, useEdificeClient } from '@edifice.io/react';
import {
  FavoritesContainer,
  LastInfosContainer,
  MessageFlashListContainer,
  NotificationListContainer,
  SchoolSpaceContainer,
  UserSpaceContainer,
} from '@edifice.io/react/homepage';
import { BetaSwitchContainer } from '~/components/BetaSwitch/BetaSwitchContainer';
import { useNotificationsLayout } from './hooks/useNotificationsLayout';

/** Check old format URL and redirect if needed */
export const loader = async () => {
  return null;
};

export const Root = () => {
  const { init } = useEdificeClient();
  const { isSidebarOpen, toggleNotifications, closeNotifications } =
    useNotificationsLayout();

  if (!init) return <LoadingScreen position={false} />;

  return (
    <PageLayout
      scrollMode="columns"
      variant="fullpage"
      noPadding={{
        sidebarRight: true,
      }}
    >
      <PageLayout.Header onNotificationsClick={toggleNotifications} />
      <PageLayout.SidebarLeft className="bg-white">
        <div className="d-flex flex-column py-16 gap-16 ">
          <SchoolSpaceContainer />
          <LastInfosContainer />
        </div>
      </PageLayout.SidebarLeft>
      <PageLayout.Content>
        <div className="d-flex flex-column py-16 gap-16">
          <BetaSwitchContainer />
          <MessageFlashListContainer />
          <UserSpaceContainer>
            <FavoritesContainer />
          </UserSpaceContainer>
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
          closeButton={true}
          onClose={closeNotifications}
          backdrop={true}
        >
          <NotificationListContainer />
        </PageLayout.Overlay>
      )}
    </PageLayout>
  );
};

export default Root;
