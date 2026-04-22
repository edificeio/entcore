import type { ReactNode } from 'react';

export interface ListWidgetItem {
  id: string;
  icon?: ReactNode;
  imageUrl?: string;
  label: string;
  sublabel?: string;
  href?: string;
}
