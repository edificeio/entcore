import { OnboardingModal } from '@edifice.io/react/modals';
import illuOnboardingIncoming from '~/assets/illu-onboarding-incoming.svg';
import illuOnboardingNew from '~/assets/illu-onboarding-new.svg';
import illuOnboardingRecall from '~/assets/illu-onboarding-recall.svg';
import illuOnboardingSearch from '~/assets/illu-onboarding-search.svg';
import { useI18n } from '~/hooks/useI18n';

export default function MessageOnboardingModal() {
  const { t } = useI18n();

  return (
    <OnboardingModal
      id="showOnboardingConversation"
      items={[
        {
          src: illuOnboardingNew,
          title: t('onboarding.modal.screen1.title'),
          alt: t('onboarding.modal.screen1.alt'),
          text: t('onboarding.modal.screen1.text'),
        },
        {
          src: illuOnboardingSearch,
          title: t('onboarding.modal.screen2.title'),
          alt: t('onboarding.modal.screen2.alt'),
          text: t('onboarding.modal.screen2.text'),
        },
        {
          src: illuOnboardingRecall,
          title: t('onboarding.modal.screen3.title'),
          alt: t('onboarding.modal.screen3.alt'),
          text: t('onboarding.modal.screen3.text'),
        },
        {
          src: illuOnboardingIncoming,
          title: t('onboarding.modal.screen4.title'),
          alt: t('onboarding.modal.screen4.alt'),
          text: t('onboarding.modal.screen4.text'),
        },
      ]}
    />
  );
}
