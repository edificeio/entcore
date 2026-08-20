import {
  ButtonBeta as Button,
  Flex,
  LoadingScreen,
  PageLayout,
  useBreakpoint,
  useEdificeClient,
} from '@edifice.io/react';

import { IconArrowLeft } from '@edifice.io/react/icons';

import { useI18n } from '~/hooks/useI18n';
import './customize.css';

/** Check old format URL and redirect if needed */
export const loader = async () => {
  return null;
};

export const Component = () => {
  const { init } = useEdificeClient();
  const { md } = useBreakpoint();
  const { common_t } = useI18n();

  if (!init) return <LoadingScreen position={false} />;

  return (
    <PageLayout
      scrollMode="page"
      variant="centered"
      noPadding={{ content: true, sidebarRight: true, sidebarLeft: true }}
    >
      <PageLayout.Header />
      <PageLayout.Content className="customize-content">
        <Flex direction="column" gap="16">
          <div>
            <Button
              data-testid="customize-back-button"
              className="customize-back-button"
              leftIcon={<IconArrowLeft />}
              variant="ghost"
            >
              {common_t('back')}
            </Button>
            <h1>{common_t('navbar.customize')}</h1>
          </div>
        </Flex>
      </PageLayout.Content>

      {md && (
        <PageLayout.SidebarRight className="customize-right">
          <></>
        </PageLayout.SidebarRight>
      )}
    </PageLayout>
  );
};
