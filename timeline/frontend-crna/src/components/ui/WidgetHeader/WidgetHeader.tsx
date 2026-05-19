import { Heading } from '@edifice.io/react';
import clsx from 'clsx';
import type { ReactNode } from 'react';
import './WidgetHeader.css';

export interface WidgetHeaderProps {
  title?: string;
  action?: ReactNode;
  className?: string;
  titleClassName?: string;
}

export function WidgetHeader({
  title,
  action,
  className,
  titleClassName,
}: WidgetHeaderProps) {
  if (!title && !action) return null;
  return (
    <div className={clsx('widget-header-container', className)}>
      {title && (
        <Heading
          level="h4"
          headingStyle="h5"
          className={clsx('widget-header-title', titleClassName)}
        >
          {title}
        </Heading>
      )}
      {action && <div className="widget-header-action">{action}</div>}
    </div>
  );
}
