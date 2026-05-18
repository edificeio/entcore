import { TextSkeleton } from '@edifice.io/react';

export interface WidgetSkeletonProps {
  count?: number;
}

export function WidgetSkeleton({ count = 3 }: WidgetSkeletonProps) {
  return (
    <div className="d-flex flex-column gap-8">
      {Array.from({ length: count }, (_, i) => (
        <TextSkeleton key={i} size="md" />
      ))}
    </div>
  );
}
