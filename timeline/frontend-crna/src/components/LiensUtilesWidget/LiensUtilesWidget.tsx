import React from 'react';
import { useTranslation } from 'react-i18next';
import { ListWidget, type ListWidgetItem } from '../ListWidget';

export interface LiensUtilesWidgetProps {
  items?: ListWidgetItem[];
  isLoading?: boolean;
}

export function LiensUtilesWidget({ items = [], isLoading = false }: LiensUtilesWidgetProps) {
  const { t } = useTranslation();

  return (
    <ListWidget
      title={t('homepage.widget.liens-utiles.title', 'Liens utiles')}
      items={items}
      isLoading={isLoading}
      style={{ '--list-widget-bg': 'var(--edifice-danger-200)' } as React.CSSProperties}
    />
  );
}
