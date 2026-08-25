import Masonry from 'antd/es/masonry';
import type { MasonryProps } from 'antd/es/masonry';
import { Children, type ReactNode } from 'react';

interface WidgetMasonryProps {
  children: ReactNode;
  columns?: MasonryProps['columns'];
  gutter?: MasonryProps['gutter'];
}

export function WidgetMasonry({
  children,
  columns = { xs: 1, sm: 1, md: 1, lg: 2 },
  gutter = 16,
}: WidgetMasonryProps) {
  const items = Children.toArray(children).map((child, index) => ({
    key: index,
    data: null,
    children: child,
  }));

  return (
    <Masonry
      columns={columns}
      gutter={gutter}
      items={items}
      itemRender={(item) => <>{item.children}</>}
      // Widgets load their content asynchronously (skeleton -> real data),
      // changing height after the initial layout. Without `fresh`, Masonry
      // only re-measures on breakpoint/item-list changes, so items keep
      // stale positions and end up overlapping or jumping columns once a
      // resize eventually triggers a recompute.
      fresh
    />
  );
}
