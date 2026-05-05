import { type ReactNode } from 'react';
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
  onSeeMore?: () => void;
  backgroundColor?: string;
}

export function ListWidget({
  title,
  items,
  onSeeMore,
  backgroundColor = '#f7f7f7',
}: ListWidgetProps) {
  return (
    <div className="list-widget" style={{ backgroundColor }}>
      <div className="list-widget-header">
        <p className="list-widget-title">{title}</p>
        {onSeeMore && (
          <button className="list-widget-see-more" onClick={onSeeMore}>
            Voir plus
            <svg width="20" height="20" viewBox="0 0 20 20" fill="none" aria-hidden="true">
              <path
                d="M11 3h6v6M16.5 3.5L9 11M8 5H4a1 1 0 0 0-1 1v10a1 1 0 0 0 1 1h10a1 1 0 0 0 1-1v-4"
                stroke="currentColor"
                strokeWidth="1.5"
                strokeLinecap="round"
                strokeLinejoin="round"
              />
            </svg>
          </button>
        )}
      </div>
      <ul className="list-widget-list">
        {items.map((item) => {
          const content = (
            <>
              {item.icon && <div className="list-widget-item-icon">{item.icon}</div>}
              <div className="list-widget-item-text">
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
                <a href={item.href} className="list-widget-item">
                  {content}
                </a>
              ) : (
                <div className="list-widget-item">{content}</div>
              )}
            </li>
          );
        })}
      </ul>
    </div>
  );
}
