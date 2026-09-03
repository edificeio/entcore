import { DYSLEXIC_FONT_ID } from '~/models/customization';
import { render, screen } from '~/mocks/setup';
import { CustomizationPreview } from './CustomizationPreview';

const requiredProps = {
  selectedLanguage: 'fr',
};

describe('CustomizationPreview', () => {
  it('renders the French texts for selectedLanguage="fr"', () => {
    render(<CustomizationPreview {...requiredProps} />);

    expect(screen.getByText('Bonjour')).toBeInTheDocument();
    expect(screen.getByText('Dernières actualités')).toBeInTheDocument();
  });

  it('renders the translated texts for another selectedLanguage', () => {
    render(<CustomizationPreview selectedLanguage="en" />);

    expect(screen.getByText('Hello')).toBeInTheDocument();
    expect(screen.getByText('Latest news')).toBeInTheDocument();
    expect(screen.queryByText('Bonjour')).not.toBeInTheDocument();
    expect(screen.queryByText('Dernières actualités')).not.toBeInTheDocument();
  });

  it('falls back to French for an unknown selectedLanguage', () => {
    render(<CustomizationPreview selectedLanguage="unknown" />);

    expect(screen.getByText('Bonjour')).toBeInTheDocument();
    expect(screen.getByText('Dernières actualités')).toBeInTheDocument();
  });

  it('has no focusable element (purely decorative preview)', () => {
    render(<CustomizationPreview {...requiredProps} />);

    expect(screen.queryAllByRole('button')).toHaveLength(0);
    expect(screen.queryAllByRole('link')).toHaveLength(0);
  });

  it('does not apply the dyslexic font class by default', () => {
    const { container } = render(<CustomizationPreview {...requiredProps} />);

    expect(container.querySelector('.customization-preview')).not.toHaveClass(
      'ff-dyslexic',
    );
  });

  it('applies the ff-dyslexic class when selecterFontName is the dyslexic font', () => {
    const { container } = render(
      <CustomizationPreview
        {...requiredProps}
        selecterFontName={DYSLEXIC_FONT_ID}
      />,
    );

    expect(container.querySelector('.customization-preview')).toHaveClass(
      'ff-dyslexic',
    );
  });

  it('does not apply the dyslexic font class for another selecterFontName', () => {
    const { container } = render(
      <CustomizationPreview {...requiredProps} selecterFontName="default" />,
    );

    expect(container.querySelector('.customization-preview')).not.toHaveClass(
      'ff-dyslexic',
    );
  });
});
