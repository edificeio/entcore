import { LoadingScreen, PageLayout, useEdificeClient } from '@edifice.io/react';
import {
  LastInfosContainer,
  MessageFlashListContainer,
  SchoolSpaceContainer,
} from '@edifice.io/react/homepage';

/** Check old format URL and redirect if needed */
export const loader = async () => {
  return null;
};

export const Root = () => {
  const { init } = useEdificeClient();

  if (!init) return <LoadingScreen position={false} />;

  return init ? (
    <PageLayout scrollMode="columns" variant="fullpage">
      <PageLayout.Header />
      <PageLayout.SidebarLeft className="d-grid align-content-start bg-white py-16 gap-16">
        <SchoolSpaceContainer />
        <LastInfosContainer />
      </PageLayout.SidebarLeft>
      <PageLayout.Content className="d-grid py-16 gap-16">
        <MessageFlashListContainer />
      </PageLayout.Content>
    </PageLayout>
  ) : null;
};

export default Root;
