import {
  ButtonBeta as Button,
  Flex,
  LoadingScreen,
  ModalBeta as Modal,
  PageLayout,
  useBackground,
  useBreakpoint,
  useEdificeClient,
} from '@edifice.io/react';

import { IconArrowLeft } from '@edifice.io/react/icons';

import { useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { CustomizationForm } from '~/components/CustomizationForm';
import { CustomizationPreview } from '~/components/CustomizationPreview/CustomizationPreview';
import { useCustomizationForm } from '~/hooks/useCustomizationForm';
import { useI18n } from '~/hooks/useI18n';
import { useUnsavedChangesGuard } from '~/hooks/useUnsavedChangesGuard';
import { HOME_PATH } from '~/routes/paths';
import './customize.css';

/** Check old format URL and redirect if needed */
export const loader = async () => {
  return null;
};

export const Component = () => {
  const { init } = useEdificeClient();
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const { md, lg } = useBreakpoint();
  const { t, common_t } = useI18n();
  const { background, productOverride } = useBackground();

  const { isDirty, saveChanges, isSaving, ...form } = useCustomizationForm();
  const [isLeaveModalOpen, setIsLeaveModalOpen] = useState(false);
  const { selectedBackground, selectedFont, selectedLanguage } = form;
  const { blocker, bypass } = useUnsavedChangesGuard(isDirty);
  const isNavigationBlocked = blocker.state === 'blocked';
  const showLeaveModal = isLeaveModalOpen || isNavigationBlocked;

  if (!init) return <LoadingScreen position={false} />;

  const handleSaveClick = () => {
    // Saving reloads the page: do not warn about the changes just saved.
    saveChanges({ onBeforeReload: bypass });
  };

  const handleBackClick = () => {
    if (isDirty) {
      setIsLeaveModalOpen(true);
      return;
    }
    goBack();
  };

  const handleLeaveConfirm = () => {
    setIsLeaveModalOpen(false);
    if (isNavigationBlocked) {
      blocker.proceed();
      return;
    }
    bypass();
    goBack();
  };

  const handleLeaveCancel = () => {
    setIsLeaveModalOpen(false);
    if (isNavigationBlocked) blocker.reset();
  };

  /** Go to the previous page, or to the home page when there is none (e.g. new tab). */
  const goToPreviousPage = () => {
    if (window.history.length > 1) {
      navigate(-1);
      return;
    }
    navigate(HOME_PATH);
  };

  const goBack = () => {
    // Go back to URL in the callback query param, if any.
    const callback = searchParams.get('callback');

    if (!callback) {
      goToPreviousPage();
      return;
    }

    try {
      const callbackUrl = new URL(callback, location.origin);

      if (callbackUrl.origin !== location.origin) {
        goToPreviousPage();
        return;
      }

      window.location.assign(callbackUrl.href);
    } catch {
      goToPreviousPage();
    }
  };

  return (
    <PageLayout
      scrollMode="columns"
      variant="centered"
      noPadding={{ content: true, sidebarRight: true, sidebarLeft: true }}
      data-product={productOverride}
      data-background={background}
    >
      <PageLayout.Header />
      <PageLayout.Content
        className="customize-content"
        data-background={undefined}
      >
        <div className="customize-content-wrap">
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
                <h3 className="customize-content-wrap-title">
                  {common_t('navbar.customize')}
                </h3>
              </div>

              <CustomizationForm form={form} />

              <Flex
                direction="row"
                gap="8"
                justify="end"
                align="center"
                className="w-100"
              >
                <Button
                  variant="filled"
                  onClick={handleSaveClick}
                  disabled={!isDirty || isSaving}
                  isLoading={isSaving}
                >
                  {common_t('save')}
                </Button>
              </Flex>
            </Flex>
            {md && (
              <CustomizationPreview
                selectedFont={selectedFont}
                selectedLanguage={selectedLanguage}
                selectedBackground={selectedBackground}
              />
            )}
          </Flex>
        </div>
      </PageLayout.Content>
      <Modal
        id="customize-leave-modal"
        size="m"
        isOpen={showLeaveModal}
        onModalClose={handleLeaveCancel}
      >
        <Modal.Header onModalClose={handleLeaveCancel}>
          {t('homepage.customize.leave.title')}
        </Modal.Header>
        <Modal.Body>{t('homepage.customize.leave.body')}</Modal.Body>
        <Modal.Footer>
          <Button
            data-testid="customize-leave-cancel-button"
            variant="ghost"
            onClick={handleLeaveConfirm}
          >
            {common_t('cancel')}
          </Button>
          <Button
            data-testid="customize-leave-back-button"
            variant="filled"
            onClick={handleLeaveCancel}
          >
            {t('homepage.customize.leave.back')}
          </Button>
        </Modal.Footer>
      </Modal>
    </PageLayout>
  );
};
