import { describe, expect, it } from 'vitest';

import { render, screen } from '~/mocks/setup';
import { DecisionComment } from './DecisionComment';

describe('DecisionComment', () => {
  it('shows its title then the comment as plain text', async () => {
    render(<DecisionComment title="Note interne" comment={'Ligne 1\nLigne 2'} />);

    expect(await screen.findByText('Note interne')).toBeVisible();
    const text = screen.getByTestId('reports-decision-comment-text');
    expect(text).toHaveTextContent('Ligne 1 Ligne 2');
    // Decision comments are plain text: typed line breaks are not kept
    expect(text).toHaveStyle({ whiteSpace: 'normal' });
  });

  it('shows the recipients line only when given', async () => {
    const { rerender } = render(
      <DecisionComment title="Note interne" comment="Texte" />,
    );
    await screen.findByText('Texte');
    expect(
      screen.queryByTestId('reports-decision-comment-recipients'),
    ).not.toBeInTheDocument();

    rerender(
      <DecisionComment
        title="À l'auteur du contenu"
        recipients="Alex Morel"
        comment="Texte"
      />,
    );
    expect(
      screen.getByTestId('reports-decision-comment-recipients'),
    ).toHaveTextContent('Alex Morel');
  });

  it('shows "Aucun commentaire" for an empty comment', async () => {
    render(<DecisionComment title="Note interne" comment="" />);

    expect(
      await screen.findByTestId('reports-decision-comment-text'),
    ).toHaveTextContent('Aucun commentaire');
  });
});
