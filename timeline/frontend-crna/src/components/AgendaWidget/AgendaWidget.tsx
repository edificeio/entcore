import { EmptyScreen } from '@edifice.io/react';
import { HomeCard } from '@edifice.io/react/homepage';
import { IconArrowRight } from '@edifice.io/react/icons';
import { useTranslation } from 'react-i18next';
import { useAgenda } from '~/hooks/useAgenda';
import { WidgetSkeleton } from '../ui/WidgetSkeleton';
import { AgendaDayGroup } from './AgendaDayGroup';
import illuNoEvent from './assets/illu-no-event.svg';
import './AgendaWidget.css';

export interface AgendaWidgetProps {
  onSeeMore?: () => void;
}

export function AgendaWidget({
  onSeeMore = () => window.open('/calendar', '_self'),
}: AgendaWidgetProps) {
  const { t } = useTranslation('timeline');
  const { dayGroups, isLoading, isError } = useAgenda();

  return (
    <HomeCard variant="user">
      <HomeCard.Header
        title={t('homepage.crna.widget.agenda.title', 'Agenda')}
        actionLabel={t('homepage.crna.widget.see.more', 'Voir plus')}
        onActionClick={onSeeMore}
        actionRightIcon={<IconArrowRight />}
      />
      <HomeCard.Content>
        {isLoading ? (
          <WidgetSkeleton />
        ) : isError || dayGroups.length === 0 ? (
          <EmptyScreen
            imageSrc={illuNoEvent}
            size={64}
            text={
              isError
                ? t(
                    'homepage.crna.widget.agenda.error',
                    'Impossible de récupérer votre agenda. Veuillez réessayer plus tard.',
                  )
                : t(
                    'homepage.crna.widget.agenda.empty',
                    "Pas d'évènement à venir.",
                  )
            }
          />
        ) : (
          <div className="agenda-content">
            {dayGroups.map((group) => (
              <AgendaDayGroup key={group.date} group={group} />
            ))}
          </div>
        )}
      </HomeCard.Content>
    </HomeCard>
  );
}
