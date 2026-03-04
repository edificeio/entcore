import { OnboardingModal } from '@edifice.io/react/modals';
import { DisplayRuleCheckResult } from 'node_modules/@edifice.io/react/dist/modules/modals/OnboardingModal/OnboardingModal';
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
      id="showOnboardingClean"
      items={[
        {
          src: illuOnboardingClean,
          title: t('onboarding.modal.clean.title'),
          alt: t('onboarding.modal.clean.title'),
          text: "<strong>L’application Messagerie intégrera bientôt une automatisation de suppression des anciens messages.</strong><br />Afin de limiter l'utilisation d'espace de stockage, les messages de plus de 2 ans seront supprimés s’ils ne sont classés dans aucun dossier.Cette action se fera automatiquement et régulièrement tout au long de l'année. <br />Cela libérera de l'espace sur votre quota de stockage. Cette mesure permettra de libérer jusqu’à 8 tonnes d’équivalent CO2 par an sur toutes nos plateformes. Un bon geste pour la planète ! <br />Cette automatisation sera appliquée à partir du xxx 2026. Pensez à faire du tri dès que possible !",
        },
      ]}
      onDisplayRuleCheck={onDisplayRuleCheck}
    />
  );
}
