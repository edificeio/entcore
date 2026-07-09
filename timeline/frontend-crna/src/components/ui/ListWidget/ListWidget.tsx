import { HomeCard } from '@edifice.io/react/homepage';
import { IconArrowRight, IconExternalLink } from '@edifice.io/react/icons';
import clsx from 'clsx';
import React from 'react';
import { useTranslation } from 'react-i18next';
import type { ListWidgetItem, WidgetBaseProps } from '~/models';
import { WidgetEmptyState } from '../WidgetEmptyState';
import { WidgetSkeleton } from '../WidgetSkeleton';
import './ListWidget.css';

export type { ListWidgetItem } from '~/models';

export interface ListWidgetProps extends WidgetBaseProps {
  title: string;
  items: ListWidgetItem[];
  style?: React.CSSProperties;
  externalLink?: boolean;
  filter?: React.ReactNode;
  /** Rendered after the list/empty/error content, inside HomeCard.Content. */
  footer?: React.ReactNode;
  /** When true, renders errorState instead of the item list. */
  isError?: boolean;
  /** Overrides the default WidgetEmptyState when items is empty. */
  emptyState?: React.ReactNode;
  /** Rendered when isError is true. */
  errorState?: React.ReactNode;
  /** Extra class applied to each item, on top of list-widget-item. */
  itemClassName?: string;
}

export function ListWidget({
  title,
  items,
  isLoading = false,
  isError = false,
  onSeeMore,
  externalLink = false,
  style,
  filter,
  footer,
  emptyState,
  errorState,
  itemClassName,
}: ListWidgetProps) {
  const { t } = useTranslation();
  return (
    <HomeCard variant="user" style={style}>
      <HomeCard.Header
        title={title}
        actionLabel={onSeeMore ? t('homepage.crna.widget.see.more', 'Voir plus') : undefined}
        onActionClick={onSeeMore}
        actionRightIcon={externalLink ? <IconExternalLink /> : <IconArrowRight />}
      />
      <HomeCard.Content>
        {filter}
        {isLoading ? (
          <WidgetSkeleton />
        ) : isError ? (
          (errorState ?? <WidgetEmptyState text={t('homepage.crna.widget.error', 'Une erreur est survenue')} />)
        ) : items.length === 0 ? (
          (emptyState ?? <WidgetEmptyState />)
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
                    <a
                      href={item.href}
                      className={clsx('list-widget-item link-discret', itemClassName)}
                      {...(externalLink ? { target: '_blank', rel: 'noreferrer' } : {})}
                    >
                      {content}
                    </a>
                  ) : (
                    <div className={clsx('list-widget-item', itemClassName)}>{content}</div>
                  )}
                </li>
              );
            })}
          </ul>
        )}
        {footer}
      </HomeCard.Content>
    </HomeCard>
  );
}
