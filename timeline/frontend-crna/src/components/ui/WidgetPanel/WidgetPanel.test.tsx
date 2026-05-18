import { describe, expect, it } from 'vitest';
import { render, screen } from '~/mocks/setup';
import { WidgetPanel } from './WidgetPanel';

describe('WidgetPanel', () => {
  it('renders children', () => {
    render(<WidgetPanel>Contenu</WidgetPanel>);
    expect(screen.getByText('Contenu')).toBeInTheDocument();
  });

  it('renders the title when provided', () => {
    render(<WidgetPanel title="Mon panneau">Contenu</WidgetPanel>);
    expect(screen.getByText('Mon panneau')).toBeInTheDocument();
  });

  it('does not render a header when title and action are both absent', () => {
    const { container } = render(<WidgetPanel>Contenu</WidgetPanel>);
    expect(container.querySelector('.widget-panel-header')).toBeNull();
  });

  it('renders the header when action is provided without title', () => {
    render(<WidgetPanel action={<button>Action</button>}>Contenu</WidgetPanel>);
    expect(screen.getByRole('button', { name: 'Action' })).toBeInTheDocument();
  });

  it('applies custom className to the root element', () => {
    const { container } = render(<WidgetPanel className="extra">Contenu</WidgetPanel>);
    expect(container.firstChild).toHaveClass('widget-panel', 'extra');
  });
});
