import type { ReactNode } from 'react';

export interface LinkItem {
  id: string;
  icon?: ReactNode;
  imageUrl?: string;
  label: string;
  sublabel?: string;
  href: string;
}
