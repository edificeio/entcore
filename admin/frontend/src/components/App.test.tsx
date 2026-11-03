import { describe, expect, it } from 'vitest';
import { render, screen } from '~/mocks/setup';
import { App } from './App';

describe('App', () => {
  it('should render', async () => {
    render(<App />);

    const button = await screen.findByRole(
      'button',
      { name: /button/i },
      { timeout: 5000 },
    );

    expect(button).toBeInTheDocument();
    expect(button).toHaveTextContent('Button');
  });
});
