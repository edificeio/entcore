import { DYSLEXIC_FONT_ID } from '~/models/customization';
import { render, screen } from '~/mocks/setup';
import { CustomizationPreview } from './CustomizationPreview';

describe('CustomizationPreview', () => {
  it('renders the greeting and last infos texts', () => {
    render(<CustomizationPreview />);

    expect(screen.getByText('Bonjour')).toBeInTheDocument();
    expect(screen.getByText('Dernières actualités')).toBeInTheDocument();
  });

  it('has no focusable element (purely decorative preview)', () => {
    render(<CustomizationPreview />);

    expect(screen.queryAllByRole('button')).toHaveLength(0);
    expect(screen.queryAllByRole('link')).toHaveLength(0);
  });

  it('does not apply the dyslexic font class by default', () => {
    const { container } = render(<CustomizationPreview />);

    expect(container.querySelector('.customization-preview')).not.toHaveClass(
      'ff-dyslexic',
    );
  });

  it('applies the ff-dyslexic class when selecterFontName is the dyslexic font', () => {
    const { container } = render(
      <CustomizationPreview selecterFontName={DYSLEXIC_FONT_ID} />,
    );

    expect(container.querySelector('.customization-preview')).toHaveClass(
      'ff-dyslexic',
    );
  });

  it('does not apply the dyslexic font class for another selecterFontName', () => {
    const { container } = render(
      <CustomizationPreview selecterFontName="default" />,
    );

    expect(container.querySelector('.customization-preview')).not.toHaveClass(
      'ff-dyslexic',
    );
  });
});
