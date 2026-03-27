import {
  DisplayRuleCheckResult,
  OnboardingModal,
} from '@edifice.io/react/modals';
import illuOnboardingClean from '~/assets/illu-onboarding-clean.svg';
import { useI18n } from '~/hooks/useI18n';

type OnboardingModalCustomState = { type: 'Date'; value: string };

export default function MessageOnboardingCleanModal() {
  const { t } = useI18n();

  function onDisplayRuleCheck(
    previousState?: OnboardingModalCustomState,
  ): DisplayRuleCheckResult<OnboardingModalCustomState> {
    const nowUTC = new Date();

    const lastDisplayDate = previousState
      ? new Date(previousState.value)
      : undefined;
    const dateToCompare = new Date();
    dateToCompare.setMonth(5, 1); // June 1st, of the year
    let shouldDisplay = false;
    if (nowUTC.getTime() > dateToCompare.getTime()) {
      // If we're after the 1st of June of the year, we want to show the modal if it has never been shown or if it was last shown last year.
      shouldDisplay =
        !lastDisplayDate ||
        lastDisplayDate.getFullYear() < dateToCompare.getFullYear();
    }

    return {
      display: shouldDisplay,
      nextState: {
        type: 'Date',
        value: nowUTC.toISOString(),
      },
    };
  }

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
      onDisplayRuleCheck={onDisplayRuleCheck}
    />
  );
}
