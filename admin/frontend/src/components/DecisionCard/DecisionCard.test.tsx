import { describe, expect, it } from 'vitest';

import { render, screen } from '~/mocks/setup';
import { type Decision, type Status } from '~/models';
import { DecisionCard } from './DecisionCard';

const decision = (overrides: Partial<Decision> = {}): Decision => ({
  id: 'dec-1',
  userId: 'u1',
  displayName: 'Kiliane BENOIT',
  status: 'QUARANTINE',
  comment: 'Raison',
  createdAt: '2026-09-28T15:20:00',
  ...overrides,
});

describe('DecisionCard', () => {
  it('shows the decision label, date and author', async () => {
    render(<DecisionCard decision={decision()} />);

    expect(
      await screen.findByText('Contenu mis en quarantaine'),
    ).toBeVisible();
    expect(
      screen.getByText('28/09/2026 à 15:20 • Kiliane BENOIT'),
    ).toBeVisible();
  });

  it('shows the internal note once expanded', async () => {
    render(<DecisionCard decision={decision()} />);

    (
      await screen.findByRole('button', { name: 'Afficher les commentaires' })
    ).click();

    expect(await screen.findByText('Note interne')).toBeVisible();
    expect(screen.getByText('Raison')).toBeVisible();
    expect(
      screen.getByRole('button', { name: 'Masquer les commentaires' }),
    ).toHaveAttribute('aria-expanded', 'true');
  });

  it('keeps the toggle for a decision without comment', async () => {
    render(<DecisionCard decision={decision({ comment: '' })} />);

    (
      await screen.findByRole('button', { name: 'Afficher les commentaires' })
    ).click();

    expect(await screen.findByText('Aucun commentaire')).toBeVisible();
  });

  it('shows an unknown status as is', async () => {
    render(
      <DecisionCard decision={decision({ status: 'ARCHIVED' as Status })} />,
    );

    expect(await screen.findByText('ARCHIVED')).toBeVisible();
  });
});
