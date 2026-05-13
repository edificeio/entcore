import { useTranslation } from 'react-i18next';
import { ListWidget, type ListWidgetItem } from '../ListWidget';

export interface AvantagesWidgetProps {
  items?: ListWidgetItem[];
  isLoading?: boolean;
  onSeeMore?: () => void;
}

export function AvantagesWidget({
  items = [],
  isLoading = false,
  onSeeMore = () => window.open('/avantages', '_self'),
}: AvantagesWidgetProps) {
  const { t } = useTranslation();

  return (
    <ListWidget
      title={t('homepage.widget.avantages.title', 'Mes avantages')}
      items={items}
      isLoading={isLoading}
      onSeeMore={onSeeMore}
    />
  );
}
