import { useTranslation } from 'react-i18next';
import { useMediacentre } from '~/hooks/useMediacentre';
import { ListWidget } from '../ui/ListWidget';

export function MediacentreWidget({
  onSeeMore = () => window.open('/mediacentre', '_self'),
}: {
  onSeeMore?: () => void;
}) {
  const { t } = useTranslation();
  const { data: items = [], isLoading } = useMediacentre();

  return (
    <ListWidget
      title={t('homepage.widget.mediacentre.title', 'Médiacentre')}
      items={items}
      isLoading={isLoading}
      onSeeMore={onSeeMore}
    />
  );
}
