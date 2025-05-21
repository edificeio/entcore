import { OnboardingModal } from '@edifice.io/react/modals';
import illuOnboardingIncoming from '~/assets/illu-onboarding-incoming.svg';
import illuOnboardingNew from '~/assets/illu-onboarding-new.svg';
import illuOnboardingRecall from '~/assets/illu-onboarding-recall.svg';
import illuOnboardingSearch from '~/assets/illu-onboarding-search.svg';
import { useI18n } from '~/hooks';

export default function MessageOnboardingModal() {
  const { t } = useI18n();

  return (
    <OnboardingModal
      id="showOnboardingConversation"
      items={[
        {
          src: illuOnboardingNew,
          alt: t('onboarding.modal.screen1.alt'),
          text: t('onboarding.modal.screen1.text'),
        },
        {
          src: illuOnboardingSearch,
          alt: t('onboarding.modal.screen2.alt'),
          text: t('onboarding.modal.screen2.text'),
        },
        {
          src: illuOnboardingRecall,
          alt: t('onboarding.modal.screen3.alt'),
          text: t('onboarding.modal.screen3.text'),
        },
        {
          src: illuOnboardingIncoming,
          alt: t('onboarding.modal.screen4.alt'),
          text: t('onboarding.modal.screen4.text'),
        },
      ]}
      modalOptions={{
        title: t('onboarding.modal.title'),
        prevText: 'explorer.modal.onboarding.trash.prev', // to remove because optional
        nextText: 'explorer.modal.onboarding.trash.next', // to remove because optional
        closeText: 'explorer.modal.onboarding.trash.close', // to remove because optional
      }}
    />
  );
}
