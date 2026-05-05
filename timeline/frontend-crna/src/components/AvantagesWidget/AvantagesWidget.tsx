import { useTranslation } from 'react-i18next';
import { ListWidget, type ListWidgetItem } from '../ListWidget';

export interface AvantagesWidgetProps {
  items?: ListWidgetItem[];
  onSeeMore?: () => void;
}

export function AvantagesWidget({
  items = [],
  onSeeMore = () => window.open('/avantages', '_self'),
}: AvantagesWidgetProps) {
  const { t } = useTranslation();

  return (
    <ListWidget
      title={t('homepage.widget.avantages.title', 'Mes avantages')}
      items={items}
      onSeeMore={onSeeMore}
    />
  );
}
