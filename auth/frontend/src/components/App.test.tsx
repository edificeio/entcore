import { describe, expect, it } from 'vitest';
import { render, screen } from '~/mocks/setup';
import { App } from './App';

describe('App', () => {
  it('should render WayfPage', () => {
    render(<App />);
    expect(screen.getByRole('heading', { level: 1 })).toBeInTheDocument();
  });
});
