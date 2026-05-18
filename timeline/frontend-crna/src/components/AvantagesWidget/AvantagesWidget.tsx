import { useTranslation } from 'react-i18next';
import { useAvantages } from '~/hooks/useAvantages';
import { ListWidget } from '../ui/ListWidget';

export function AvantagesWidget({
  onSeeMore = () => window.open('/avantages', '_self'),
}: {
  onSeeMore?: () => void;
}) {
  const { t } = useTranslation();
  const { data: items = [], isLoading } = useAvantages();

  return (
    <ListWidget
      title={t('homepage.widget.avantages.title', 'Mes avantages')}
      items={items}
      isLoading={isLoading}
      onSeeMore={onSeeMore}
    />
  );
}
