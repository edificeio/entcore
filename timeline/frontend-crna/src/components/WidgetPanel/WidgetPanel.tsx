import { Heading } from '@edifice.io/react';
import clsx from 'clsx';
import type { ComponentPropsWithoutRef, ReactNode } from 'react';
import './WidgetPanel.css';

export interface WidgetPanelProps extends ComponentPropsWithoutRef<'div'> {
  title?: string;
  action?: ReactNode;
}

export function WidgetPanel({
  children,
  className,
  title,
  action,
  ...props
}: WidgetPanelProps) {
  return (
    <div className={clsx('widget-panel', className)} {...props}>
      {(title || action) && (
        <div className="widget-panel-header">
          {title && (
            <Heading level="h3" headingStyle="h6" className="widget-panel-title">
              {title}
            </Heading>
          )}
          {action && <div className="widget-panel-action">{action}</div>}
        </div>
      )}
      <div className="widget-panel-body">{children}</div>
    </div>
  );
}
