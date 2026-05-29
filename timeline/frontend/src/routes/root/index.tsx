import {
  LoadingScreen,
  PageLayout,
  useBreakpoint,
  useEdificeClient,
  useOverlay,
} from '@edifice.io/react';
import {
  LastInfosContainer,
  MessageFlashListContainer,
  SchoolSpaceContainer,
} from '@edifice.io/react/homepage';
import { useState } from 'react';
import { BetaSwitchContainer } from '~/components/BetaSwitch/BetaSwitchContainer';

/** Check old format URL and redirect if needed */
export const loader = async () => {
  return null;
};

export const Root = () => {
  const { init } = useEdificeClient();
  const [openSidebar, setOpenSidebar] = useState(false);
  const { md } = useBreakpoint();
  const { toggleOverlay } = useOverlay();

  const handleNotificationsToggle = () => {
    if (md) {
      setOpenSidebar((prev) => !prev);
    } else {
      toggleOverlay();
    }
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
      <PageLayout.Header onNotificationsClick={handleNotificationsToggle} />
      <PageLayout.SidebarLeft className="d-grid align-content-start bg-white py-16 gap-16">
        <SchoolSpaceContainer />
        <LastInfosContainer />
      </PageLayout.SidebarLeft>
      <PageLayout.Content className="d-grid align-content-start py-16 gap-16">
        <BetaSwitchContainer />
        <MessageFlashListContainer />
      </PageLayout.Content>

      {openSidebar ? (
        <PageLayout.SidebarRight>
          <div></div>
          {/* <NotificationListContainer
            onCloseNotifications={handleNotificationsToggle} 
        />  */}
        </PageLayout.SidebarRight>
      ) : (
        <PageLayout.Overlay
          closeButton={true}
          onClose={handleNotificationsToggle}
          backdrop={true}
        >
          <div></div>
          {/* <NotificationListContainer
          onCloseNotifications={handleNotificationsToggle} 
        /> */}
        </PageLayout.Overlay>
      )}
    </PageLayout>
  );
};

export default Root;
