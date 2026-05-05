import { useTranslation } from 'react-i18next';
import { ListWidget, type ListWidgetItem } from '../ListWidget';

export interface LiensUtilesWidgetProps {
  items?: ListWidgetItem[];
}

export function LiensUtilesWidget({ items = [] }: LiensUtilesWidgetProps) {
  const { t } = useTranslation();

  return (
    <ListWidget
      title={t('homepage.widget.liens-utiles.title', 'Liens utiles')}
      items={items}
      backgroundColor="#ffebeb"
    />
  );
}
