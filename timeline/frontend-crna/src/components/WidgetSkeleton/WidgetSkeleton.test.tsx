import { describe, expect, it } from 'vitest';
import { render } from '~/mocks/setup';
import { WidgetSkeleton } from './WidgetSkeleton';

describe('WidgetSkeleton', () => {
  it('renders 3 placeholder items by default', () => {
    const { container } = render(<WidgetSkeleton />);
    expect(container.querySelectorAll('.placeholder').length).toBe(3);
  });

  it('renders the given count of placeholder items', () => {
    const { container } = render(<WidgetSkeleton count={5} />);
    expect(container.querySelectorAll('.placeholder').length).toBe(5);
  });
});
