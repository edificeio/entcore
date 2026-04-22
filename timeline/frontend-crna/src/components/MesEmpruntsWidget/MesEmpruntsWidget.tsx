import React from 'react';
import { useTranslation } from 'react-i18next';
import { useMesEmprunts } from '~/hooks/useMesEmprunts';
import { ListWidget } from '../ui/ListWidget';
import './MesEmpruntsWidget.css';

export function MesEmpruntsWidget({
  onSeeMore = () => window.open('/mediacentre', '_self'),
}: {
  onSeeMore?: () => void;
}) {
  const { t } = useTranslation();
  const { data: items = [], isLoading } = useMesEmprunts();

  return (
    <div className="mes-emprunts-widget">
      <ListWidget
        title={t('homepage.widget.emprunts.title', 'Mes emprunts')}
        items={items}
        isLoading={isLoading}
        onSeeMore={onSeeMore}
        style={{ '--list-widget-bg': 'var(--edifice-danger-200)' } as React.CSSProperties}
      />
    </div>
  );
}
