import {
  ButtonBeta as Button,
  Flex,
  LoadingScreen,
  PageLayout,
  useBreakpoint,
  useEdificeClient,
} from '@edifice.io/react';

import { IconArrowLeft } from '@edifice.io/react/icons';

import { useNavigate } from 'react-router-dom';
import { CustomizationForm } from '~/components/CustomizationForm';
import { useCustomizationForm } from '~/hooks/useCustomizationForm';
import { useI18n } from '~/hooks/useI18n';
import './customize.css';
import { CustomizationPreview } from '~/components/CustomizationPreview/CustomizationPreview';

/** Check old format URL and redirect if needed */
export const loader = async () => {
  return null;
};

export const Component = () => {
  const { init } = useEdificeClient();
  const navigate = useNavigate();
  const { md, lg } = useBreakpoint();
  const { common_t } = useI18n();

  const { resetChanges, saveChanges, isPending, ...form } =
    useCustomizationForm();

  if (!init) return <LoadingScreen position={false} />;

  const handleSaveClick = () => {
    saveChanges();
  };

  const handleBackClick = () => {
    navigate(-1);
  };

  return (
    <PageLayout
      scrollMode="page"
      variant="centered"
      noPadding={{ content: true, sidebarRight: true, sidebarLeft: true }}
    >
      <PageLayout.Header />
      <PageLayout.Content className="customize-content">
        <Flex direction="row" gap={lg ? '64' : '32'}>
          <Flex direction="column" gap="16" align="start">
            <div>
              <Button
                data-testid="customize-back-button"
                className="customize-back-button"
                leftIcon={<IconArrowLeft />}
                variant="ghost"
                onClick={handleBackClick}
              >
                {common_t('back')}
              </Button>
              <h1>{common_t('navbar.customize')}</h1>
            </div>

            <CustomizationForm form={form} />

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
              <Button
                variant="filled"
                onClick={handleSaveClick}
                disabled={isPending}
              >
                {common_t('save')}
              </Button>
            </Flex>
          </Flex>
          {/* TODO #IMPULS-6022
              greetingText/lastInfosText are hardcoded until the form provides
              them already translated in the selected (not yet saved) language. */}
          {md && (
            <CustomizationPreview
              selecterFontName={form.selectedFont}
              greetingText="Bonjour"
              lastInfosText="Dernières actualités"
            />
          )}
        </Flex>
      </PageLayout.Content>
    </PageLayout>
  );
};
