import { EmptyScreen } from '@edifice.io/react';
import illuError from '@edifice.io/bootstrap/dist/images/emptyscreen/illu-error.svg';
import { useTranslation } from 'react-i18next';

export function CarnetDeBordErrorState() {
  const { t } = useTranslation('timeline');
  return (
    <EmptyScreen
      imageSrc={illuError}
      size={64}
      text={t(
        'homepage.crna.widget.carnet-de-bord.pronote-error',
        "Nous avons rencontré un problème avec le service Pronote. Si le problème persiste, demandez à l'administration de votre établissement de vérifier le paramétrage de Pronote.",
      )}
    />
  );
}
