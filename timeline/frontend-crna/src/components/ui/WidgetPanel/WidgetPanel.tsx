import clsx from 'clsx';
import type { ComponentPropsWithoutRef, ReactNode } from 'react';
import { WidgetHeader } from '../WidgetHeader';
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
      <WidgetHeader title={title} action={action} />
      <div className="widget-panel-body">{children}</div>
    </div>
  );
}
