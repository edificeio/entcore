import { ButtonBeta } from '@edifice.io/react';
import { IconExternalLink } from '@edifice.io/react/icons';
import React, { type ReactNode } from 'react';
import { useTranslation } from 'react-i18next';
import type { WidgetBaseProps } from '../types';
import { WidgetHeader } from '../WidgetHeader';
import { WidgetSkeleton } from '../WidgetSkeleton';
import './ListWidget.css';

export interface ListWidgetItem {
  id: string;
  icon?: ReactNode;
  label: string;
  sublabel?: string;
  href?: string;
}

export interface ListWidgetProps extends WidgetBaseProps {
  title: string;
  items: ListWidgetItem[];
  style?: React.CSSProperties;
}

export function ListWidget({
  title,
  items,
  isLoading = false,
  onSeeMore,
  style,
}: ListWidgetProps) {
  const { t } = useTranslation();
  return (
    <div className="list-widget" style={style}>
      <WidgetHeader
        className="list-widget-header"
        titleClassName="list-widget-title"
        title={title}
        action={
          onSeeMore ? (
            <ButtonBeta
              color="default"
              variant="ghost"
              rightIcon={<IconExternalLink />}
              onClick={onSeeMore}
            >
              {t('homepage.widget.see.more', 'Voir plus')}
            </ButtonBeta>
          ) : undefined
        }
      />

      {isLoading ? (
        <WidgetSkeleton />
      ) : items.length === 0 ? (
        <p className="align-items-center gap-8 d-flex flex-column">
          {t('homepage.widget.list.empty', 'Aucun élément à afficher')}
        </p>
      ) : (
        <ul className="list-widget-list">
          {items.map((item) => {
            const content = (
              <>
                {item.icon && (
                  <div className="list-widget-item-icon">{item.icon}</div>
                )}
                <div className="d-flex flex-column gap-4 list-widget-item-text">
                  <span className="list-widget-item-label">{item.label}</span>
                  {item.sublabel && (
                    <span className="list-widget-item-sublabel">
                      {item.sublabel}
                    </span>
                  )}
                </div>
              </>
            );

            return (
              <li key={item.id}>
                {item.href ? (
                  <a href={item.href} className="list-widget-item link-discret">
                    {content}
                  </a>
                ) : (
                  <div className="list-widget-item">{content}</div>
                )}
              </li>
            );
          })}
        </ul>
      )}
    </div>
  );
}
