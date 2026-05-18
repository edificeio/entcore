import type { ReactNode } from 'react';

export interface ListWidgetItem {
  id: string;
  icon?: ReactNode;
  label: string;
  sublabel?: string;
  href?: string;
}
