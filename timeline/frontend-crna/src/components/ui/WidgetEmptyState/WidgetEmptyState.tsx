import { EmptyScreen } from '@edifice.io/react';
import illuNoContent from '@edifice.io/bootstrap/dist/images/emptyscreen/illu-no-content-in-folder.svg';
import { useTranslation } from 'react-i18next';

interface WidgetEmptyStateProps {
  text?: string;
}

export function WidgetEmptyState({ text }: WidgetEmptyStateProps) {
  const { t } = useTranslation();
  return (
    <EmptyScreen
      imageSrc={illuNoContent}
      text={text ?? t('homepage.widget.empty', 'Aucun élément disponible')}
      size={80}
    />
  );
}
