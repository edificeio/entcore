import { describe, expect, it } from 'vitest';
import { render, screen } from '~/mocks/setup';
import { WidgetHeader } from './WidgetHeader';

describe('WidgetHeader', () => {
  it('renders the title', () => {
    render(<WidgetHeader title="Mon widget" />);
    expect(screen.getByText('Mon widget')).toBeInTheDocument();
  });

  it('renders the action slot when provided', () => {
    render(<WidgetHeader title="Test" action={<button>Voir plus</button>} />);
    expect(screen.getByRole('button', { name: 'Voir plus' })).toBeInTheDocument();
  });

  it('does not render an action slot when omitted', () => {
    render(<WidgetHeader title="Test" />);
    expect(screen.queryByRole('button')).toBeNull();
  });

  it('forwards className to the grid wrapper', () => {
    const { container } = render(<WidgetHeader title="Test" className="my-class" />);
    expect(container.firstChild).toHaveClass('my-class');
  });

  it('applies titleClassName to the heading', () => {
    render(<WidgetHeader title="Test" titleClassName="my-title-class" />);
    expect(screen.getByText('Test')).toHaveClass('my-title-class');
  });
});
