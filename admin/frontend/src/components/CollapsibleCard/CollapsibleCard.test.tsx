import { describe, expect, it } from 'vitest';

import {
  render,
  screen,
  waitForElementToBeRemoved,
} from '~/mocks/setup';
import { CollapsibleCard } from './CollapsibleCard';

const renderCard = (
  props: { defaultExpanded?: boolean; testId?: string } = {},
) => (
  <CollapsibleCard
    header={<span>Titre</span>}
    expandLabel="Afficher"
    collapseLabel="Masquer"
    defaultExpanded={props.defaultExpanded}
    data-testid={props.testId}
  >
    <p>Contenu {props.testId}</p>
  </CollapsibleCard>
);

describe('CollapsibleCard', () => {
  it('is collapsed by default', async () => {
    render(renderCard());

    const toggle = await screen.findByRole('button', { name: 'Afficher' });
    expect(toggle).toHaveAttribute('aria-expanded', 'false');
    expect(screen.getByText('Titre')).toBeVisible();
    expect(screen.queryByText(/Contenu/)).not.toBeInTheDocument();
  });

  it('expands and collapses on click', async () => {
    render(renderCard());

    (await screen.findByRole('button', { name: 'Afficher' })).click();

    const toggle = await screen.findByRole('button', { name: 'Masquer' });
    expect(toggle).toHaveAttribute('aria-expanded', 'true');
    const panel = document.getElementById(
      toggle.getAttribute('aria-controls') ?? '',
    );
    expect(panel).toContainElement(screen.getByText(/Contenu/));

    toggle.click();
    await waitForElementToBeRemoved(() => screen.queryByText(/Contenu/));
    expect(screen.getByRole('button', { name: 'Afficher' })).toHaveAttribute(
      'aria-expanded',
      'false',
    );
  });

  it('uses a native button so Enter and Space work', async () => {
    render(renderCard());

    const toggle = await screen.findByRole('button', { name: 'Afficher' });
    expect(toggle.tagName).toBe('BUTTON');
    expect(toggle).toHaveAttribute('type', 'button');
  });

  it('describes its toggle with the header, so identical labels stay distinct', async () => {
    render(renderCard());

    expect(
      await screen.findByRole('button', { name: 'Afficher' }),
    ).toHaveAccessibleDescription('Titre');
  });

  it('opens with defaultExpanded', async () => {
    render(renderCard({ defaultExpanded: true }));

    expect(await screen.findByText(/Contenu/)).toBeVisible();
    expect(screen.getByRole('button', { name: 'Masquer' })).toHaveAttribute(
      'aria-expanded',
      'true',
    );
  });

  it('keeps each card independent', async () => {
    render(
      <>
        {renderCard({ testId: 'a' })}
        {renderCard({ testId: 'b' })}
      </>,
    );

    (await screen.findByTestId('a-toggle')).click();

    expect(await screen.findByText('Contenu a')).toBeVisible();
    expect(screen.getByTestId('b-toggle')).toHaveAttribute(
      'aria-expanded',
      'false',
    );
    expect(screen.queryByText('Contenu b')).not.toBeInTheDocument();
  });
});
