import { useTranslation } from 'react-i18next';
import { ListWidget, type ListWidgetItem } from '../ListWidget';

export interface MediacentreWidgetProps {
  items?: ListWidgetItem[];
  onSeeMore?: () => void;
}

export function MediacentreWidget({
  items = [],
  onSeeMore = () => window.open('/mediacentre', '_self'),
}: MediacentreWidgetProps) {
  const { t } = useTranslation();

  return (
    <ListWidget
      title={t('homepage.widget.mediacentre.title', 'Médiacentre')}
      items={items}
      onSeeMore={onSeeMore}
    />
  );
}
