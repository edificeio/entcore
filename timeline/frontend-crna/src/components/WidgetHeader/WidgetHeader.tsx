import { Grid, Heading } from '@edifice.io/react';
import clsx from 'clsx';
import type { ReactNode } from 'react';
import './WidgetHeader.css';

export interface WidgetHeaderProps {
  title: string;
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
  return (
    <Grid className={clsx('align-items-center', className)}>
      <Grid.Col sm="2" lg="6">
        <Heading
          level="h3"
          headingStyle="h5"
          className={clsx('widget-header-title', titleClassName)}
        >
          {title}
        </Heading>
      </Grid.Col>
      <Grid.Col sm="2" lg="6" className="d-flex justify-content-end">
        {action}
      </Grid.Col>
    </Grid>
  );
}
