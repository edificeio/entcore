import { LoadingScreen, PageLayout, useEdificeClient } from '@edifice.io/react';
import {
  LastInfosContainer,
  MessageFlashListContainer,
  SchoolSpaceContainer,
} from '@edifice.io/react/homepage';
import { BetaSwitchContainer } from '~/components/BetaSwitch/BetaSwitchContainer';

/** Check old format URL and redirect if needed */
export const loader = async () => {
  return null;
};

export const Root = () => {
  const { init } = useEdificeClient();

  if (!init) return <LoadingScreen position={false} />;

  return (
    <PageLayout scrollMode="columns" variant="fullpage">
      <PageLayout.Header />
      <PageLayout.SidebarLeft className="d-grid align-content-start bg-white py-16 gap-16">
        <SchoolSpaceContainer />
        <LastInfosContainer />
      </PageLayout.SidebarLeft>
      <PageLayout.Content className="d-grid align-content-start py-16 gap-16">
        <BetaSwitchContainer />
        <MessageFlashListContainer />
      </PageLayout.Content>
    </PageLayout>
  );
};

export default Root;
