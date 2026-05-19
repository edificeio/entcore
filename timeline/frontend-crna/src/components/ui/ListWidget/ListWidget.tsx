import { ButtonBeta } from '@edifice.io/react';
import { IconArrowRight, IconExternalLink } from '@edifice.io/react/icons';
import React from 'react';
import { useTranslation } from 'react-i18next';
import type { ListWidgetItem, WidgetBaseProps } from '~/models';
import { WidgetEmptyState } from '../WidgetEmptyState';
import { WidgetHeader } from '../WidgetHeader';
import { WidgetSkeleton } from '../WidgetSkeleton';
import './ListWidget.css';

export type { ListWidgetItem } from '~/models';

export interface ListWidgetProps extends WidgetBaseProps {
  title: string;
  items: ListWidgetItem[];
  style?: React.CSSProperties;
  externalLink?: boolean;
}

export function ListWidget({
  title,
  items,
  isLoading = false,
  onSeeMore,
  externalLink = false,
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
              rightIcon={ externalLink ? <IconExternalLink /> : <IconArrowRight /> }
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
        <WidgetEmptyState />
      ) : (
        <ul className="list-widget-list">
          {items.map((item) => {
            const content = (
              <>
                {item.icon ? (
                  <div className="list-widget-item-icon">{item.icon}</div>
                ) : item.imageUrl ? (
                  <div className="list-widget-item-icon">
                    <img src={item.imageUrl} alt="" />
                  </div>
                ) : null}
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
