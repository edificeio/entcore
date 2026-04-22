import { Heading } from '@edifice.io/react';
import clsx from 'clsx';
import { type ComponentPropsWithoutRef, type ReactNode } from 'react';
import './WidgetCard.css';

export interface WidgetCardProps extends ComponentPropsWithoutRef<'div'> {
  backgroundColor?: string;
  title?: string;
  action?: ReactNode;
  footerAction?: ReactNode;
  footerBackgroundColor?: string;
}

export function WidgetCard({
  children,
  className,
  backgroundColor = '#ffffff',
  style,
  title,
  action,
  footerAction,
  footerBackgroundColor,
  ...props
}: WidgetCardProps) {
  return (
    <div
      className={clsx('widget-card', className)}
      style={{ backgroundColor, ...style }}
      {...props}
    >
      {(title || action) && (
        <div className="widget-card-header">
          {title && (
            <Heading level="h6" headingStyle="h6" className="widget-card-title">
              {title}
            </Heading>
          )}
          {action && <div className="widget-card-action">{action}</div>}
        </div>
      )}
      <div className="widget-card-body">{children}</div>
      {footerAction && (
        <div
          className="widget-card-footer"
          style={footerBackgroundColor ? { backgroundColor: footerBackgroundColor } : undefined}
        >
          {footerAction}
        </div>
      )}
    </div>
  );
}
