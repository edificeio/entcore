import { describe, expect, it } from 'vitest';
import { render, screen } from '~/mocks/setup';
import { App } from './App';

describe('App', () => {
  it('should render', () => {
    render(<App />);

    const button = screen.getByRole('button', { name: /button/i });

    expect(button).toBeInTheDocument();
    expect(button).toHaveTextContent('Button');

    // screen.debug();
  });
});
