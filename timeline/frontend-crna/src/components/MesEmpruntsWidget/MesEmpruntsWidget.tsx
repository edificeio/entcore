import React from 'react';
import { useTranslation } from 'react-i18next';
import { ListWidget, type ListWidgetItem } from '../ListWidget';
import './MesEmpruntsWidget.css';

export interface MesEmpruntsWidgetProps {
  items?: ListWidgetItem[];
  onSeeMore?: () => void;
}

export function MesEmpruntsWidget({
  items = [],
  onSeeMore = () => window.open('/mediacentre', '_self'),
}: MesEmpruntsWidgetProps) {
  const { t } = useTranslation();

  return (
    <div className="mes-emprunts-widget">
      <ListWidget
        title={t('homepage.widget.emprunts.title', 'Mes emprunts')}
        items={items}
        onSeeMore={onSeeMore}
        style={{ '--list-widget-bg': 'var(--edifice-danger-200)' } as React.CSSProperties}
      />
    </div>
  );
}
