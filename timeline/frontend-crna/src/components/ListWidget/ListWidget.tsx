import { Button, Grid, Heading, TextSkeleton } from '@edifice.io/react';
import { IconExternalLink } from '@edifice.io/react/icons';
import React, { type ReactNode } from 'react';
import './ListWidget.css';

export interface ListWidgetItem {
  id: string;
  icon?: ReactNode;
  label: string;
  sublabel?: string;
  href?: string;
}

export interface ListWidgetProps {
  title: string;
  items: ListWidgetItem[];
  isLoading?: boolean;
  onSeeMore?: () => void;
  style?: React.CSSProperties;
}

export function ListWidget({
  title,
  items,
  isLoading = false,
  onSeeMore,
  style,
}: ListWidgetProps) {
  return (
    <div className="list-widget" style={style}>
      <Grid className="align-items-center list-widget-header">
        <Grid.Col sm="2" lg="6">
          <Heading level="h3" headingStyle="h5" className="list-widget-title">
            {title}
          </Heading>
        </Grid.Col>
        <Grid.Col sm="2" lg="6" className="d-flex justify-content-end">
          {onSeeMore && (
            <Button
              variant="ghost"
              size="sm"
              className="list-widget-see-more"
              rightIcon={<IconExternalLink />}
              onClick={onSeeMore}
            >
              Voir plus
            </Button>
          )}
        </Grid.Col>
      </Grid>

      {isLoading ? (
        <div className="d-flex flex-column gap-8">
          <TextSkeleton size="md" />
          <TextSkeleton size="md" />
          <TextSkeleton size="sm" />
        </div>
      ) : items.length === 0 ? (
        <p className="list-widget-empty">Aucun élément à afficher</p>
      ) : (
        <ul className="list-widget-list">
          {items.map((item) => {
            const content = (
              <>
                {item.icon && <div className="list-widget-item-icon">{item.icon}</div>}
                <div className="d-flex flex-column gap-4 list-widget-item-text">
                  <span className="list-widget-item-label">{item.label}</span>
                  {item.sublabel && (
                    <span className="list-widget-item-sublabel">{item.sublabel}</span>
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
