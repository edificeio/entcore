import { OnboardingModal } from '@edifice.io/react/modals';
import illuOnboardingClean from '~/assets/illu-onboarding-clean.svg';
import { useI18n } from '~/hooks/useI18n';

export default function MessageOnboardingCleanModal() {
  const { t } = useI18n();

  return (
    <OnboardingModal
      id="showOnboardingConversation"
      items={[
        {
          src: illuOnboardingClean,
          title: t('onboarding.modal.clean.title'),
          alt: t('onboarding.modal.clean.title'),
          text: t('onboarding.modal.clean.text'),
        },
      ]}
    />
  );
}
