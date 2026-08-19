import {
  LoadingScreen,
  PageLayout,
  useBreakpoint,
  useEdificeClient,
} from '@edifice.io/react';

/** Check old format URL and redirect if needed */
export const loader = async () => {
  return null;
};

export const Component = () => {
  const { init } = useEdificeClient();
  const { md } = useBreakpoint();

  if (!init) return <LoadingScreen position={false} />;

  return (
    <PageLayout
      scrollMode="columns"
      variant="fullpage"
      noPadding={{
        sidebarRight: true,
      }}
    >
      {md && <PageLayout.Header />}
      <PageLayout.SidebarLeft>
        <></>
      </PageLayout.SidebarLeft>
      <PageLayout.Content>
        <></>
      </PageLayout.Content>

      <PageLayout.SidebarRight>
        <></>
      </PageLayout.SidebarRight>
    </PageLayout>
  );
};
