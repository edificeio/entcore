import {
  ButtonBeta,
  LoadingScreen,
  PageLayout,
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
import { IconClose } from '@edifice.io/react/icons';
import { useTranslation } from 'react-i18next';
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
  const { t } = useTranslation();

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
          <ButtonBeta
            aria-label={t('close')}
            color="tertiary"
            leftIcon={<IconClose />}
            type="button"
            variant="ghost"
            title={t('close')}
            onClick={closeNotifications}
            className="pagelayout-sidebar-close-button"
            data-testid="pagelayout-sidebar-close-button"
          />
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
          <NotificationListContainer
            onCloseNotifications={closeNotifications}
          />
        </PageLayout.Overlay>
      )}
    </PageLayout>
  );
};

export default Root;
