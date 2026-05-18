import { Heading } from '@edifice.io/react';
import clsx from 'clsx';
import React, { type ComponentPropsWithoutRef, type ReactNode } from 'react';
import './WidgetCard.css';

export interface WidgetCardProps extends ComponentPropsWithoutRef<'div'> {
  title?: string;
  action?: ReactNode;
  footerAction?: ReactNode;
  footerStyle?: React.CSSProperties;
}

export function WidgetCard({
  children,
  className,
  style,
  title,
  action,
  footerAction,
  footerStyle,
  ...props
}: WidgetCardProps) {
  return (
    <div
      className={clsx('widget-card', className)}
      style={style}
      {...props}
    >
      {(title || action) && (
        <div className="widget-card-header">
          {title && (
            <Heading level="h3" headingStyle="h6" className="widget-card-title">
              {title}
            </Heading>
          )}
          {action && <div className="widget-card-action">{action}</div>}
        </div>
      )}
      <div className="widget-card-body">{children}</div>
      {footerAction && (
        <div className="widget-card-footer" style={footerStyle}>
          {footerAction}
        </div>
      )}
    </div>
  );
}
