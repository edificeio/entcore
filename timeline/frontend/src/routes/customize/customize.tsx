import {
  ButtonBeta as Button,
  Flex,
  LoadingScreen,
  PageLayout,
  useBreakpoint,
  useEdificeClient,
} from '@edifice.io/react';

import { IconArrowLeft } from '@edifice.io/react/icons';

import { CustomizeForm } from '~/components/CustomizeForm/CustomizeForm';
import { useCustomizeForm } from '~/hooks/useCustomizeForm';
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

  const { resetChanges, saveChanges, ...form } = useCustomizeForm();

  if (!init) return <LoadingScreen position={false} />;

  const handleSaveClick = () => {
    saveChanges();
  };

  return (
    <PageLayout
      scrollMode="page"
      variant="centered"
      noPadding={{ content: true, sidebarRight: true, sidebarLeft: true }}
    >
      <PageLayout.Header />
      <PageLayout.Content className="customize-content">
        <Flex direction="column" gap="16" align="start">
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

          <CustomizeForm form={form} />

          <Flex
            direction="row"
            gap="8"
            justify="end"
            align="center"
            className="w-100"
          >
            <Button variant="ghost" onClick={resetChanges}>
              {common_t('cancel')}
            </Button>
            <Button variant="filled" onClick={handleSaveClick}>
              {common_t('save')}
            </Button>
          </Flex>
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
