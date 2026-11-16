import { describe, expect, it } from 'vitest';

import { render, screen, within } from '~/mocks/setup';
import { type Decision } from '~/models';
import { DecisionsHistory } from './DecisionsHistory';

const decision = (
  id: string,
  displayName: string,
  createdAt: string,
): Decision => ({
  id,
  userId: `user-${id}`,
  displayName,
  status: 'QUARANTINE',
  comment: '',
  createdAt,
});

const unordered = () => [
  decision('1', 'A', '2026-09-28T12:00:00'),
  decision('2', 'B', '2026-09-28T10:00:00'),
  decision('3', 'C', '2026-09-28T11:00:00'),
];

describe('DecisionsHistory', () => {
  it('lists the decisions from the oldest', async () => {
    render(<DecisionsHistory decisions={unordered()} />);

    const cards = await screen.findAllByTestId('reports-decision-card');
    expect(
      cards.map((card) => within(card).getByText(/•/).textContent?.slice(-1)),
    ).toEqual(['B', 'C', 'A']);
    expect(screen.getByRole('list')).toHaveAttribute('role', 'list');
    expect(screen.getAllByRole('listitem')).toHaveLength(3);
    expect(
      screen.getByRole('heading', {
        level: 2,
        name: 'Historique des traitements',
      }),
    ).toBeVisible();
    expect(
      screen.getByText('Décisions et commentaires enregistrés pour ce contenu.'),
    ).toBeVisible();
  });

  it('does not mutate the given decisions', async () => {
    const decisions = unordered();
    render(<DecisionsHistory decisions={decisions} />);

    await screen.findAllByTestId('reports-decision-card');
    expect(decisions.map((d) => d.id)).toEqual(['1', '2', '3']);
  });

  it('shows the empty state without decisions', async () => {
    render(<DecisionsHistory decisions={[]} />);

    expect(
      await screen.findByText('Aucun historique pour le moment'),
    ).toBeVisible();
    expect(
      screen.getByText(
        "Aucune décision n'a encore été enregistrée pour ce contenu. Les décisions et leurs commentaires apparaîtront ici après le premier traitement.",
      ),
    ).toBeVisible();
    expect(
      screen.queryByRole('heading', { name: 'Historique des traitements' }),
    ).not.toBeInTheDocument();
  });

  it('expands each decision independently', async () => {
    render(<DecisionsHistory decisions={unordered().slice(0, 2)} />);

    const toggles = await screen.findAllByRole('button', {
      name: 'Afficher les commentaires',
    });
    toggles[0].click();

    expect(
      await screen.findByRole('button', { name: 'Masquer les commentaires' }),
    ).toBeVisible();
    expect(toggles[1]).toHaveAttribute('aria-expanded', 'false');
  });
});
