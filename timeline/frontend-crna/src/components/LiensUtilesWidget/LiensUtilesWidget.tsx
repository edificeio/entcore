import React from 'react';
import { useTranslation } from 'react-i18next';
import { useLiensUtiles } from '~/hooks/useLiensUtiles';
import { ListWidget } from '../ui/ListWidget';

export function LiensUtilesWidget() {
  const { t } = useTranslation();
  const { data: items = [], isLoading } = useLiensUtiles();

  return (
    <ListWidget
      title={t('homepage.widget.liens-utiles.title', 'Liens utiles')}
      items={items}
      isLoading={isLoading}
      style={{ '--list-widget-bg': 'var(--edifice-danger-200)' } as React.CSSProperties}
    />
  );
}
