import { Masonry } from 'antd';
import type { MasonryProps } from 'antd';
import { Children, type ReactNode } from 'react';

interface WidgetMasonryProps {
  children: ReactNode;
  columns?: MasonryProps['columns'];
  gutter?: MasonryProps['gutter'];
}

export function WidgetMasonry({
  children,
  columns = { xs: 1, sm: 1, md: 2, lg: 2 },
  gutter = 16,
}: WidgetMasonryProps) {
  const items =
    Children.map(children, (child, index) => ({
      key: index,
      data: null,
      children: child,
    })) ?? [];

  return (
    <Masonry
      columns={columns}
      gutter={gutter}
      items={items}
      itemRender={(item) => <>{item.children}</>}
    />
  );
}
